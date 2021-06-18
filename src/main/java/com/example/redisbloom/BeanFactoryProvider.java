package com.example.redisbloom;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;

/**
 * @author ZhuYX
 * @date 2021/06/15
 */
@Configuration
public class BeanFactoryProvider implements BeanFactoryPostProcessor {
    private static ConfigurableListableBeanFactory listableBeanFactory;

    public static void autowire(Object t) {
        listableBeanFactory.autowireBeanProperties(t, ConfigurableListableBeanFactory.AUTOWIRE_BY_TYPE, false);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        listableBeanFactory = beanFactory;
    }
}
