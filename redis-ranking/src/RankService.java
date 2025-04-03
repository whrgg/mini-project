
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.resps.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class RankService {
    private  final String RANK_KEY;
    private  final JedisPool jedisPool;
    private static final JedisPoolConfig jedisPoolConfig;
    //分片标记
    private  boolean shareMark =false;
    //固定分法
    private  final double SCORE_RANGE = 1000.0;
    private int sharkIndex = 0;
    private boolean isMigrating = false; // 新增：迁移状态标志
    static {
        jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(10);
        jedisPoolConfig.setMaxTotal(10);
        jedisPoolConfig.setMaxTotal(100);

        // 最大空闲连接数
        jedisPoolConfig.setMaxIdle(10);

        // 最小空闲连接数
        jedisPoolConfig.setMinIdle(5);

        // 当连接池资源耗尽时，调用者最大等待时间（毫秒）
        jedisPoolConfig.setMaxWaitMillis(3000);

        // 在获取连接时，是否进行有效性检查
        jedisPoolConfig.setTestOnBorrow(true);

        // 在归还连接时，是否进行有效性检查
        jedisPoolConfig.setTestOnReturn(true);

        // 在空闲时，是否进行有效性检查
        jedisPoolConfig.setTestWhileIdle(true);

        // 每次空闲连接检查时，检查的连接数量
        jedisPoolConfig.setNumTestsPerEvictionRun(10);
    }

    public RankService(String host,int port,String key_preix) {
        RANK_KEY=key_preix;
        jedisPool =new JedisPool(jedisPoolConfig,host,port);
    }

    // 添加或更新用户分数
    public void addScore(String userId, double score) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (isMigrating) {
                // 使用事务确保双写原子性
                jedis.watch(RANK_KEY);
                String newKey = getShark(score);
                Transaction tx = jedis.multi();
                tx.zadd(RANK_KEY, score, userId);
                tx.zadd(newKey, score, userId);
                tx.exec();
            } else {
                String key = (!shareMark) ? RANK_KEY : getShark(score);
                jedis.zadd(key, score, userId);
            }
        } catch (Exception e) {
            // 重试机制
            retryAddScore(userId, score, 3); // 最多重试3次
        }
    }

    // 重试添加分数
    private void retryAddScore(String userId, double score, int retryCount) {
        if (retryCount <= 0) {
            // 记录日志或抛出异常
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            if (isMigrating) {
                String newKey = getShark(score);
                jedis.zadd(RANK_KEY, score, userId);
                jedis.zadd(newKey, score, userId);
            } else {
                String key = (!shareMark) ? RANK_KEY : getShark(score);
                jedis.zadd(key, score, userId);
            }
        } catch (Exception e) {
            retryAddScore(userId, score, retryCount - 1);
        }
    }

    // 迁移完成后进行数据校验
    private void validateMigration() {
        try (Jedis jedis = jedisPool.getResource()) {
            // 检查旧key是否为空
            if (jedis.exists(RANK_KEY)) {
                throw new RuntimeException("Migration validation failed: old key still exists");
            }

            // 检查总用户数是否一致
            long oldCount = jedis.zcard(RANK_KEY);
            long newCount = getTotalUsers();
            if (oldCount != newCount) {
                throw new RuntimeException("Migration validation failed: user count mismatch");
            }
        }
    }

    // 优化后的迁移方法
    public void migrate() {
        isMigrating = true; // 开始迁移
        shareMark = true;   // 启用分片

        try (Jedis jedis = jedisPool.getResource()) {
            long total = jedis.zcard(RANK_KEY);
            int batchSize = 10000; // 每批迁移的数据量
            long cursor = 0;

            while (cursor < total) {
                // 分批获取数据
                List<Tuple> batch = jedis.zrangeWithScores(RANK_KEY, cursor, cursor + batchSize - 1);

                // 迁移当前批次
                for (Tuple tuple : batch) {
                    String userId = tuple.getElement();
                    double score = tuple.getScore();
                    String newKey = getShark(score);
                    jedis.zadd(newKey, score, userId);
                }

                cursor += batchSize;

                // 监控内存使用情况
                if (isMemoryCritical(jedis)) {
                    pauseMigration();
                }
            }

            // 迁移完成后删除旧key
            jedis.del(RANK_KEY);
            isMigrating = false; // 结束迁移
        }
    }

    // 检查内存是否达到临界值
    private boolean isMemoryCritical(Jedis jedis) {
        String memoryInfo = jedis.info("memory");
        // 解析内存使用情况
        long usedMemory = parseUsedMemory(memoryInfo);
        long maxMemory = parseMaxMemory(memoryInfo);

        // 如果未设置最大内存，默认使用系统内存的80%作为阈值
        if (maxMemory == 0) {
            maxMemory = (long) (Runtime.getRuntime().maxMemory() * 0.8);
        }

        // 内存使用率超过90%时认为达到临界值
        double memoryUsage = (double) usedMemory / maxMemory;
        return memoryUsage > 0.9;
    }

    // 解析已使用内存
    private long parseUsedMemory(String memoryInfo) {
        for (String line : memoryInfo.split("\n")) {
            if (line.startsWith("used_memory:")) {
                return Long.parseLong(line.substring("used_memory:".length()).trim());
            }
        }
        return 0;
    }

    // 解析最大内存
    private long parseMaxMemory(String memoryInfo) {
        for (String line : memoryInfo.split("\n")) {
            if (line.startsWith("maxmemory:")) {
                return Long.parseLong(line.substring("maxmemory:".length()).trim());
            }
        }
        return 0;
    }

    // 暂停迁移
    private void pauseMigration() {
        try {
            Thread.sleep(5000); // 暂停5秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 获取用户排名（从高到低，0开始）
    public Long getUserRank(String userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (shareMark) {
                // 先获取该用户的分数
                Double userScore = null;
                for (int i = 0; i <= sharkIndex; i++) {
                    String key = RANK_KEY + i;
                    userScore = jedis.zscore(key, userId);
                    if (userScore != null) {
                        break;
                    }
                }
                if (userScore == null) {
                    return null;
                }

                // 统计分数高于该用户的总人数
                long higherRankCount = 0;
                for (int i = 0; i <= sharkIndex; i++) {
                    String key = RANK_KEY + i;
                    // 获取该分片中分数高于用户分数的人数
                    List<Tuple> higherScoredUsers = jedis.zrevrangeByScoreWithScores(key, "+inf", "(" + userScore);
                    higherRankCount += higherScoredUsers.size();
                }
                return higherRankCount;
            }
            return jedis.zrank(RANK_KEY, userId);
        }
    }

    // 获取用户分数
    public Double getUserScore(String userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (shareMark) {
                for (int i = 0; i <= sharkIndex; i++) {
                    String key = RANK_KEY + i;
                    Double score = jedis.zscore(key, userId);
                    if (score != null) {
                        return score;
                    }
                }
                return null;
            }
            return jedis.zscore(RANK_KEY, userId);
        }
    }

    // 获取前N名用户
    public List<Tuple> getTopN(int n) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (shareMark) {
                Set<Tuple> allTuples = new TreeSet<>((t1, t2) -> Double.compare(t2.getScore(), t1.getScore()));
                for (int i = 0; i <= sharkIndex; i++) {
                    String key = RANK_KEY + i;
                    List<Tuple> tuples = jedis.zrevrangeWithScores(key, 0, -1);
                    allTuples.addAll(tuples);
                }
                List<Tuple> topN = new ArrayList<>();
                int count = 0;
                for (Tuple tuple : allTuples) {
                    if (count >= n) {
                        break;
                    }
                    topN.add(tuple);
                    count++;
                }
                return topN;
            }
            return new ArrayList<>(jedis.zrevrangeWithScores(RANK_KEY, 0, n - 1));
        }
    }

    // 获取总用户数
    public Long getTotalUsers() {
        if(shareMark==true){
            try(Jedis jedis = jedisPool.getResource()){
                Long total=0L;
                for(int i=0;i<=sharkIndex;i++){
                    total+=jedis.zcard(RANK_KEY+i);
                }
                return total;
            }
        }
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zcard(RANK_KEY);
        }
    }

    public void delUserRank() {
        try (Jedis jedis = jedisPool.getResource()) {
            if (shareMark) {
                for (int i = 0; i <= sharkIndex; i++) {
                    jedis.del(RANK_KEY + i);
                }
            } else {
                jedis.del(RANK_KEY);
            }
        }
    }

    // 关闭连接
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    /*
      更新排行榜设计的人数过多上千万之后容易变成大key
      此时考虑将一个key的数据分解
     */

    /**
     * 用于获取分片的key
     * @param score
     * @return
     */
    private String getShark(double score){
        int shardIndex = (int) (score / SCORE_RANGE);
        sharkIndex=(sharkIndex<=shardIndex)?shardIndex:sharkIndex;
        return RANK_KEY + shardIndex;
    }



    public boolean isShareMark() {
        return shareMark;
    }
}