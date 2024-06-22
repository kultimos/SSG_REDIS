package com.kul.mylock;

import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.sql.Time;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 自研redis分布式锁,实现了Lock接口
 */
public class RedisDistributedLock implements Lock {

    private StringRedisTemplate redisTemplate;
    private String lockName;
    private String uuidValue;
    private long expireTime;

    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate, String lockName) {
        this.redisTemplate = stringRedisTemplate;
        this.lockName = lockName;
        this.uuidValue = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();
        this.expireTime = 50l;
    }

    @Override
    public void lock() {
        this.tryLock();
    }

    @Override
    public boolean tryLock() {
        try {
            return this.tryLock(-1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if(time == -1) {
            String script = "if redis.call('exists',KEYS[1]) == 0 or redis.call('hexists',KEYS[1],ARGV[1]) == 1 then " +
                    "redis.call('hincrby',KEYS[1],ARGV[1],1) " +
                    "redis.call('expire',KEYS[1],ARGV[2]) " +
                    "return 1 " +
                    "else " +
                    "return 0 " +
                    "end";
            while(!redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList(lockName), uuidValue, expireTime)) {
                Thread.sleep(100);
            }
            return true;
        }
        return false;
    }


    @Override
    public void unlock() {
        String script = "if redis.call('hexists',KEYS[1],ARGV[1]) == 0 then " +
                "return nil " +
                "elseif redis.call('hincrby',KEYS[1],ARGV[1],-1) == 0 then " +
                "return redis.call('del',KEYS[1]) " +
                "else " +
                "return 0 " +
                "end";
        Long flag = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuidValue);
        if(flag == null) {
            throw new RuntimeException("this lock doesn't exists");
        }
    }







    @Override
    public Condition newCondition() {
        return null;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }
}
