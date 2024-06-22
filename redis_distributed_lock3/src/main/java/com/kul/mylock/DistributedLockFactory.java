package com.kul.mylock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;

@Component
public class DistributedLockFactory {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String lockName;

    public Lock getDistributedLock(String lockType) {
        this.lockName = "redisLock";
        return new RedisDistributedLock(redisTemplate, lockName);
    }
}
