package com.kul.mylock;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.sql.Time;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 自研redis分布式锁,实现了Lock接口
 */
@Slf4j
public class RedisDistributedLock implements Lock {

    private StringRedisTemplate redisTemplate;
    private String lockName;
    private String uuidValue;
    private String expireTime;

    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate, String lockName) {
        this.redisTemplate = stringRedisTemplate;
        this.lockName = lockName;
        this.uuidValue = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();
        this.expireTime = "50";
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
                Thread.sleep(10);
            }
            renewExpire();// 获取锁以后就开始监控,进而进行锁的续期
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
        log.info("释放锁的结果是:{} ", flag);
        if(flag == null) {
            throw new RuntimeException("this lock doesn't exists");
        }
    }


    /**
     * 简易的锁自动续期方法
     */
    private void renewExpire() {
        String script = "if redis.call('HEXISTS',KEYS[1],ARGV[1]) == 1 then " +
                "return redis.call('expire',KEYS[1],ARGV[2]) " +
                "else " +
                "return 0 " +
                "end";
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if( redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuidValue, expireTime)) {
                    renewExpire();
                }
            }
        }, (Integer.parseInt(this.expireTime) * 1000) / 3);
    }




    @Override
    public Condition newCondition() {
        return null;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }
}
