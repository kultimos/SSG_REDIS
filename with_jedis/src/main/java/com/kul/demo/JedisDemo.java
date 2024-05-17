package com.kul.demo;


import redis.clients.jedis.Jedis;

public class JedisDemo {
    public static void main(String[] args) {
        // 通过指定ip端口获取redis连接
        Jedis jedis = new Jedis("192.168.10.132", 6379);

        // 指定访问服务器的密码
        jedis.auth("1234567a");

        System.out.println(jedis.ping());
        jedis.set("nba", "joikc");
        jedis.lpush("test_list", "a","s","tes","jdg","blg","skt");
    }
}
