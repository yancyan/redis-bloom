package com.example.redisbloom;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author ZhuYX
 * @date 2021/06/17
 */
@Configuration(proxyBeanMethods = false)
public class RedissonConfig implements SmartInitializingSingleton {

    @Resource
    RedisProperties redisProperties;

    @Bean
    public RedissonClient redissonClient() {
        var config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(10);

        return Redisson.create(config);
    }

    @Override
    public void afterSingletonsInstantiated() {

        BeanFactoryProvider.autowire(RedissonFactory.class);
    }
}
