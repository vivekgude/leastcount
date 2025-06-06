package com.vivekgude.leastcount.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPool;

import static com.vivekgude.leastcount.constants.Constants.PONG;

@Component
@Slf4j
public class RedisHealthCheck implements CommandLineRunner {

    private final JedisPool jedisPool;

    public RedisHealthCheck(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public void run(String... args) {
        try (var jedis = jedisPool.getResource()) {
            String pong = jedis.ping();
            if (PONG.equals(pong)) {
                log.info("Successfully connected to Redis server");
            } else {
                log.error("Failed to connect to Redis server");
                throw new RuntimeException("Redis connection test failed");
            }
        } catch (Exception e) {
            log.error("Error connecting to Redis server - {}", e.getMessage());
            throw new RuntimeException("Failed to connect to Redis server", e);
        }
    }
} 