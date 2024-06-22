package com.kul.service.impl;

import cn.hutool.core.util.IdUtil;
import com.kul.mylock.RedisDistributedLock;
import com.kul.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${server.port}")
    private String port;

    private static final String KEY = "inventory001";

    private Lock lock = new RedisDistributedLock(redisTemplate, KEY);


    /**
     * V3.0版本,其实V2.3版本已经可以作为一个简单的分布式锁来进行使用;
     * 但对于更大规模的并发量和更大规模的任务处理,我们在V2.3版本的分布式锁的基础上,仍然有很多优化点
     * 即: 可重入和自动续期两部分;
     * 3.0版本我们着重解决分布式锁的重入问题
     * 参考AQS的实现方案,我们决定通过hash的key来帮我们实现加锁和重入的能力
     * 并且因为使用hash结构进行加减锁有大量的redis语句,所以我们也需要将lua脚本整合进入我们的java程序中;
     * 这里我们采用重写一个RedisDistributedLock作为我们自研的分布式锁来使用,代码与早期2.0版本一致,但是所引的lock和unlock方法却是我们自己编写的;
     */
    @Override
    public String sale() {
        String retMessage = "";
        lock.lock();
        try {
            Thread.sleep(10);
            String result = redisTemplate.opsForValue().get(KEY);
            Integer inventoryNumber = result == null && !result.equals("") ? 0 :  Integer.parseInt(result);
            if(inventoryNumber > 0) {
                redisTemplate.opsForValue().set(KEY, String.valueOf(--inventoryNumber));
                retMessage = "端口" + port + "成功卖出一个商品,库存剩余:" + inventoryNumber;
                log.info("当前编号: {}" + Thread.currentThread().getName());
                log.info("服务端口号:{}," + retMessage, port);
                log.info("=======================");
            } else {
                retMessage = "商品卖完了,o(╥﹏╥)o";
                log.info(retMessage);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        return retMessage;
    }


    /**
     * v1.0版,单机版加锁配合nginx和jmeter压测后,不满足高并发分布式锁的性能要求,出现超卖
     */
//    @Override
//    public String sale() {
//        String retMessage = "";
//        lock.lock();
//        try {
//            Thread.sleep(10);
//            String result = redisTemplate.opsForValue().get(KEY);
//            Integer inventoryNumber = result == null && !result.equals("") ? 0 :  Integer.parseInt(result);
//            if(inventoryNumber > 0) {
//                redisTemplate.opsForValue().set(KEY, String.valueOf(--inventoryNumber));
//                retMessage = "端口" + port + "成功卖出一个商品,库存剩余:" + inventoryNumber;
//                log.info("当前编号: {}" + Thread.currentThread().getName());
//                log.info("服务端口号:{}," + retMessage, port);
//                log.info("=======================");
//            } else {
//                retMessage = "商品卖完了,o(╥﹏╥)o";
//                log.info(retMessage);
//            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            lock.unlock();
//        }
//        return retMessage;
//    }

    /**
     * 2.0版,该版本引入了setnx命令,并且可以满足在高并发场景下500条记录的安全消费,但是存在问题
     * 1.高并发场景下禁止使用递归,因为非常容易造成StackOverFlowError,所以递归一定要去除;
     * 2.使用if存在虚假唤醒的可能(虽然在当前场景下不会出现这种情况),但我们还是建议使用while替换if
     * @return
     */
//    @Override
//    public String sale() {
//        String retMessage = "";
//        String key = "zzyyRedisLock";
//        String uuid = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();
//        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, uuid);
//        if (flag) {
//            try {
//                String result = redisTemplate.opsForValue().get(KEY);
//                Integer inventoryNumber = result == null && !result.equals("") ? 0 : Integer.parseInt(result);
//                if (inventoryNumber > 0) {
//                    redisTemplate.opsForValue().set(KEY, String.valueOf(--inventoryNumber));
//                    retMessage = "端口" + port + "成功卖出一个商品,库存剩余:" + inventoryNumber;
//                    log.info("当前编号: {}" + Thread.currentThread().getName());
//                    log.info("服务端口号:{}," + retMessage, port);
//                    log.info("=======================");
//                } else {
//                    retMessage = "商品卖完了,o(╥﹏╥)o";
//                    log.info(retMessage);
//                }
//            } finally {
//                redisTemplate.delete(key);
//            }
//        } else {
//            try {
//                Thread.sleep(10);
//                this.sale();
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        return retMessage;
//    }

    /**
     * V2.1版,改进点:
     *  1.用while替换if
     *  2.用自旋代替递归重试
     * 存在的问题:
     *  如果当前服务执行过程中宕机,那么有可能分布式锁一直没有释放,为了解决这个问题,我们需要给锁增加过期时间;
     * @return
     */
//    @Override
//    public String sale() {
//        String retMessage = "";
//        String key = "zzyyRedisLock";
//        String uuid = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();
//        while (! redisTemplate.opsForValue().setIfAbsent(key, uuid)) {
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        try {
//            String result = redisTemplate.opsForValue().get(KEY);
//            Integer inventoryNumber = result == null && !result.equals("") ? 0 : Integer.parseInt(result);
//            if (inventoryNumber > 0) {
//                redisTemplate.opsForValue().set(KEY, String.valueOf(--inventoryNumber));
//                retMessage = "端口" + port + "成功卖出一个商品,库存剩余:" + inventoryNumber;
//                log.info("当前编号: {}" + Thread.currentThread().getName());
//                log.info("服务端口号:{}," + retMessage, port);
//                log.info("=======================");
//            } else {
//                retMessage = "商品卖完了,o(╥﹏╥)o";
//                log.info(retMessage);
//            }
//        } finally {
//            redisTemplate.delete(key);
//        }
//        return retMessage;
//    }

    /**
     * V2.2版,改进点:
     * 增加过期时间,避免因服务宕机导致锁永远无法被释放,这里需要注意通过setIfAbsent()方法直接设置过期时间保证了原子性
     * 仍然存在问题:
     * redisTemplate.delete(key)时,有可能删除其他线程的锁
     * 假如线程a执行任务需要1min,但是我们在代码中预设锁只有30s有效期,那么30s后锁就过期了,但是线程a不知道,还是会在1min任务执行后去删除锁,
     * 那么此时线程a删除的肯定不是自己的锁,而是其他线程正持有的锁;
     * 所以我们在释放锁之前,需要进行一次检查,检查key中存放的value是否与自己的一致,只有一致时,才释放锁,如此,便不会错误释放他人的锁;
     * 但是新的问题又来了,我们的判断操作和删除操作并非原子操作啊,而且由于没有类似putIfAbsent()的原子命令,我们就需要借助Lua脚本来实现
     * 多个语句的原子性;
     * @return
     */
//    @Override
//    public String sale() {
//        String retMessage = "";
//        String key = "zzyyRedisLock";
//        String uuid = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();
//        while (! redisTemplate.opsForValue().setIfAbsent(key, uuid, 30l, TimeUnit.SECONDS)) {
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        try {
//            String result = redisTemplate.opsForValue().get(KEY);
//            Integer inventoryNumber = result == null && !result.equals("") ? 0 : Integer.parseInt(result);
//            if (inventoryNumber > 0) {
//                redisTemplate.opsForValue().set(KEY, String.valueOf(--inventoryNumber));
//                retMessage = "端口" + port + "成功卖出一个商品,库存剩余:" + inventoryNumber;
//                log.info("当前编号: {}" + Thread.currentThread().getName());
//                log.info("服务端口号:{}," + retMessage, port);
//                log.info("=======================");
//            } else {
//                retMessage = "商品卖完了,o(╥﹏╥)o";
//                log.info(retMessage);
//            }
//        } finally {
//            if(redisTemplate.opsForValue().get(key).equalsIgnoreCase(uuid)) {
//                redisTemplate.delete(key);
//            }
//        }
//        return retMessage;
//    }

    /**
     * V2.3版本,改进点: 引入lua脚本实现多条redis命令的一致性;
     * @return
     */
//    @Override
//    public String sale() {
//        count.incrementAndGet();
//        log.info("count = {}", count);
//        String retMessage = "";
//        String key = "zzyyRedisLock";
//        String uuid = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();
//        while (! redisTemplate.opsForValue().setIfAbsent(key, uuid, 30l, TimeUnit.SECONDS)) {
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        try {
//            String result = redisTemplate.opsForValue().get(KEY);
//            Integer inventoryNumber = result == null && !result.equals("") ? 0 : Integer.parseInt(result);
//            if (inventoryNumber > 0) {
//                redisTemplate.opsForValue().set(KEY, String.valueOf(--inventoryNumber));
//                retMessage = "端口" + port + "成功卖出一个商品,库存剩余:" + inventoryNumber;
//                log.info("当前编号: {}" + Thread.currentThread().getName());
//                log.info("服务端口号:{}," + retMessage, port);
//                log.info("=======================");
//            } else {
//                retMessage = "商品卖完了,o(╥﹏╥)o";
//                log.info(retMessage);
//            }
//        } finally {
//            String luaScript =
//                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
//                            "return redis.call('del', KEYS[1])" +
//                            "else " +
//                            "return 0 " +
//                            "end";
//            redisTemplate.execute(new DefaultRedisScript(luaScript, Boolean.class), Arrays.asList(key), uuid);
//        }
//        return retMessage;
//    }
}