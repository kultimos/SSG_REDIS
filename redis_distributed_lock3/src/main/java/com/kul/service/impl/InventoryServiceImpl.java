package com.kul.service.impl;

import cn.hutool.core.util.IdUtil;
import com.kul.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    private Lock lock = new ReentrantLock();


    /**
     * V2.1版,改进点:
     * 1.用while替换if
     * 2.用自旋代替递归重试
     * @return
     */
    @Override
    public String sale() {
        String retMessage = "";
        String key = "zzyyRedisLock";
        String uuid = IdUtil.simpleUUID() + ":" + Thread.currentThread().getId();
        while (! redisTemplate.opsForValue().setIfAbsent(key, uuid)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            String result = redisTemplate.opsForValue().get(KEY);
            Integer inventoryNumber = result == null && !result.equals("") ? 0 : Integer.parseInt(result);
            if (inventoryNumber > 0) {
                redisTemplate.opsForValue().set(KEY, String.valueOf(--inventoryNumber));
                retMessage = "端口" + port + "成功卖出一个商品,库存剩余:" + inventoryNumber;
                log.info("当前编号: {}" + Thread.currentThread().getName());
                log.info("服务端口号:{}," + retMessage, port);
                log.info("=======================");
            } else {
                retMessage = "商品卖完了,o(╥﹏╥)o";
                log.info(retMessage);
            }
        } finally {
            redisTemplate.delete(key);
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
}
