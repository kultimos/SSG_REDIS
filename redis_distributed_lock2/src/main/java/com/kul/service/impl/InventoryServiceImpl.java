package com.kul.service.impl;

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
}
