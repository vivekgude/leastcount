package com.vivekgude.leastcount.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class BaseCache {

    public static final String GAME = "game:";
    public static final String PLAYER = "player:";

    @Autowired
    public JedisPool jedisPool;

    public void addKeyValue(String key, String value) {
        try {
            String result = jedisPool.getResource().set(key, value);
            log.info(result);
        } catch (Exception e) {
            log.error("Error adding key:{}, value:{}", key, value, e);
        }
    }

    public void deleteKey(String... keys) {
        try {
            long result = jedisPool.getResource().del(keys);
            log.info(String.valueOf(result));
        } catch (Exception e) {
            log.error("Error deleting key:{}", keys, e);
        }
    }

    public String getVal(String key) {
        try {
            String result = jedisPool.getResource().get(key);
            log.info(String.valueOf(result));
            return result;
        } catch (Exception e) {
            log.error("Error getting key:{}", key, e);
            return null;
        }
    }

    public void addFieldToSet(String key, String field, String value) {
        try {
            long result = jedisPool.getResource().hset(key, field, value);
            log.info(String.valueOf(result));
        } catch (Exception e) {
            log.error("Error adding field to key:{}, field:{}, value:{}", key, field, value, e);
        }
    }

    public void addFieldsToSet(String key, Map<String, String> fieldsVsValue) {
        try {
            long result = jedisPool.getResource().hset(key, fieldsVsValue);
            log.info(String.valueOf(result));
        } catch (Exception e) {
            log.error("Error adding fields to key:{}, fieldsVsValue:{}", key, fieldsVsValue, e);
        }
    }

    public String getFieldInSet(String key, String field) {
        try {
            String result = jedisPool.getResource().hget(key, field);
            log.info(String.valueOf(result));
            return result;
        } catch (Exception e) {
            log.error("Error getting field in set:{}", key);
            return null;
        }
    }

    public Map<String, String> getFieldsInSet(String key) {
        try {
            Map<String, String> result = jedisPool.getResource().hgetAll(key);
            log.info(String.valueOf(result));
            return result;
        } catch (Exception e) {
            log.error("Error getting fields in set:{}", key);
            return null;
        }
    }

    public void addValuesInList(String key, String... values) {
        try {
            long result = jedisPool.getResource().lpush(key, values);
            log.info(String.valueOf(result));
        } catch (Exception e) {
            log.error("Error adding values in List:{}", key);
        }
    }

    public List<String> getValuesInList(String key) {
        try {
            List<String> result = jedisPool.getResource().lrange(key, 0, -1);
            log.info(String.valueOf(result));
            return result;
        } catch (Exception e) {
            log.error("Error getting values in List:{}", key);
            return null;
        }
    }

}
