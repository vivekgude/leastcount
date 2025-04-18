package com.vivekgude.leastcount.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
@Slf4j
public class RedisConfig {
    @Value("${redis.server.host}")
    private String host;

    @Value("${redis.server.port}")
    private int port;

    @Bean
    public JedisPool jedisPool() {
        log.info("Creating Redis connection host:{}, port:{}", host, port);
        return new JedisPool(host, port);
    }

}
