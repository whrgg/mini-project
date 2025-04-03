import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.resps.Tuple;

import java.util.List;

public class Main {
//    public static void main(String[] args) {
//        Jedis jedis = new Jedis("localhost",6379);
//        // 如果设置 Redis 服务的密码，需要进行验证，若没有则可以省去
//        // jedis.auth("123456");
//        System.out.println("连接成功");
//        //查看服务是否运行
//        System.out.println("服务正在运行: "+jedis.ping());
//        jedis.zadd("user1",100,"twx");
//        jedis.zadd("user1",150,"wjp");
//        jedis.zadd("user1",200,"wuxiao");
//        jedis.zadd("user1",200,"wuxiao1");
//        List<Tuple> user = jedis.zrevrangeWithScores("user1",0,10);
//        for (Tuple tuple : user) {
//            System.out.println(tuple);
//        }
//        //设置 redis 字符串数据
////        jedis.set("webkey",  "swawdwa");
//        // 获取存储的数据并输出
//        System.out.println("redis 存储的字符串为: "+ jedis.get("user1"));
//    }

    public static void main(String[] args) {
        RankService service =new RankService("localhost",6379,"user");

        long l = System.currentTimeMillis();
        Long wuxiao0 = service.getUserRank("wuxiao500000");
        System.out.println(wuxiao0);
        long r = System.currentTimeMillis();
        System.out.println(r-l);


        l = System.currentTimeMillis();
         wuxiao0 = service.getUserRank("wuxiao500000");
        System.out.println(wuxiao0);
         r = System.currentTimeMillis();
        System.out.println(r-l);
//        service.addScore("wuxiao",2000);
//        service.delUserRank();
        List<Tuple> topN = service.getTopN(10);

        Long totalUsers = service.getTotalUsers();
        System.out.println(totalUsers);
        for (Tuple tuple : topN) {
            System.out.println(tuple);
        }


    }

    public static void  testadd(RankService service){
        //40s左右
        for(int i=0;i<1000000;i++){
            service.addScore("wuxiao"+i,i);
        }

    }

    public static  void testDelete(RankService service){
        service.delUserRank();
    }

}