package com.kul.servier;

import jdk.nashorn.internal.runtime.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    public static final String ORDER_KEY = "ord:";

    @Resource
    private RedisTemplate redisTemplate;

//    @Resource
//    private StringRedisTemplate stringRedisTemplate;

    public void addOrder() {
        int keyId = ThreadLocalRandom.current().nextInt(1000)+1;
        String serialNo = UUID.randomUUID().toString();
        String key = ORDER_KEY + keyId;
        String value = "京东订单" + serialNo;
        System.out.println("******key: {" + key + "}");
        System.out.println("******value: {" + value + "}");
        redisTemplate.opsForValue().set(key, value);
    }

    public String getOrderKey(Integer keyId) {
        return (String) redisTemplate.opsForValue().get(ORDER_KEY+keyId);
    }
}
