package com.littlefxc.example.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * @author fengxuechao
 * @date 2018/12/27
 **/
@EnableCaching
@Configuration
public class RedisConfig {

    /**
     * Jedis 连接工厂配置
     *
     * @return JedisConnectionFactory
     * @see JedisConnectionFactory
     */
    @Profile("jedis")
    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory();
    }

    /**
     * Lettuce 连接工厂配置
     *
     * @return LettuceConnectionFactory
     * @see LettuceConnectionFactory
     */
    @Profile("lettuce")
    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.create(connectionFactory);
    }
}
