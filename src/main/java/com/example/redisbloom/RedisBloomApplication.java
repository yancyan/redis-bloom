package com.example.redisbloom;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;
import java.util.concurrent.Executors;

@EnableScheduling
@SpringBootApplication
@EnableTransactionManagement
public class RedisBloomApplication implements SmartInitializingSingleton {

    public static void main(String[] args) {
        SpringApplication.run(RedisBloomApplication.class, args);
    }

    @Autowired
    RedisProperties redisProperties;

    @Override
    public void afterSingletonsInstantiated() {
        var thread = new Thread(new BloomFilterProvider());
        thread.start();
    }
}
