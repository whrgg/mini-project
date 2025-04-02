
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.resps.Tuple;

import java.util.List;


public class RankService {
    private  final String RANK_KEY;
    private  final JedisPool jedisPool;
    private static final JedisPoolConfig jedisPoolConfig;
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

        // 连接的最小空闲时间（毫秒），达到此时间后会进行空闲连接检查
        jedisPoolConfig.setMinEvictableIdleTimeMillis(60000);

        // 空闲连接检查的时间间隔（毫秒）
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(30000);

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
            jedis.zadd(RANK_KEY,score,userId);
        }
    }

    // 获取用户排名（从高到低，0开始）
    public Long getUserRank(String userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrank(RANK_KEY,userId);
        }
    }

    // 获取用户分数
    public Double getUserScore(String userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zscore(RANK_KEY,userId);
        }
    }

    // 获取前N名用户
    public List<Tuple> getTopN(int n) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrevrangeWithScores(RANK_KEY, 0, n - 1);
        }
    }

    // 获取总用户数
    public Long getTotalUsers() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zcard(RANK_KEY);
        }
    }

    public void delUserRank() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(RANK_KEY);
        }
    }

    // 关闭连接
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}