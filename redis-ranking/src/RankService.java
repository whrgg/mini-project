
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
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
        String key = (!shareMark) ? RANK_KEY : (RANK_KEY + getShark(score));
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zadd(key,score,userId);
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

    public void migrate(){
        shareMark= true;
        try(Jedis jedis = jedisPool.getResource()){
            List<Tuple> allUsers = jedis.zrangeWithScores(RANK_KEY, 0, -1);

            // 遍历并迁移数据
            for (Tuple tuple : allUsers) {
                String userId = tuple.getElement();
                double score = tuple.getScore();
                String newKey = getShark(score);
                jedis.zadd(newKey, score, userId);
            }

            // 删除旧key（可选）
            jedis.del(RANK_KEY);
        }

    }


    public boolean isShareMark() {
        return shareMark;
    }
}