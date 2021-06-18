package com.example.redisbloom;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author ZhuYX
 * @date 2021/06/17
 */
public class RedissonFactory {

    static RedissonClient defaultClient;

    public void setRedissonClient(@Autowired RedissonClient redissonClient) {
        defaultClient = redissonClient;
    }

    public static RedissonClient getDefaultClient() {
        return defaultClient;
    }
}
