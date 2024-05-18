package com.kul.demo;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.List;

public class LettuceDemo {
    public static void main(String[] args) {
        // 使用构建器链式编程来builder我们的redisUri
        RedisURI uri = RedisURI.builder().redis("192.168.10.132").withPort(6379)
                .withAuthentication("default", "1234567a").build();

        //创建连接客户端
        RedisClient redisClient = RedisClient.create(uri);
        StatefulRedisConnection<String, String> connect = redisClient.connect();

        // 创建操作的command
        RedisCommands commands = connect.sync();
        List keys = commands.keys("*");
        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaa" + keys);

        // 各种关闭释放资源
        connect.close();
        redisClient.shutdown();
    }
}
