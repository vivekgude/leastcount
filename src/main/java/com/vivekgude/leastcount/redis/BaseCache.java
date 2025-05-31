package com.vivekgude.leastcount.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class BaseCache {

    public static final String GAME = "game:";
    public static final String PLAYER = "player:";
    public static final String JOINED_PLAYERS = "joinedPlayers:";

    @Autowired
    private JedisPool jedisPool;

    public boolean addKeyValue(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(key, value);
            return "OK".equals(result);
        } catch (Exception e) {
            log.error("Error adding key:{}, value:{}", key, value, e);
            return false;
        }
    }

    public long deleteKeys(String... keys) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.del(keys);
        } catch (Exception e) {
            log.error("Error deleting keys:{}", (Object) keys, e);
            return 0;
        }
    }

    public String getVal(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            log.error("Error getting key:{}", key, e);
            return null;
        }
    }

    public boolean addFieldToMap(String key, String field, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            long result = jedis.hset(key, field, value);
            return result >= 0;
        } catch (Exception e) {
            log.error("Error adding field to key:{}, field:{}, value:{}", key, field, value, e);
            return false;
        }
    }

    public boolean addFieldsToMap(String key, Map<String, String> fieldsVsValue) {
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.hmset(key, fieldsVsValue);
            return "OK".equals(result);
        } catch (Exception e) {
            log.error("Error adding fields to key:{}, fieldsVsValue:{}", key, fieldsVsValue, e);
            return false;
        }
    }

    public String getFieldInMap(String key, String field) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hget(key, field);
        } catch (Exception e) {
            log.error("Error getting field in map:{}", key, e);
            return null;
        }
    }

    public Map<String, String> getFieldsInMap(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hgetAll(key);
        } catch (Exception e) {
            log.error("Error getting fields in map:{}", key, e);
            return null;
        }
    }

    public boolean addValuesInList(String key, String... values) {
        try (Jedis jedis = jedisPool.getResource()) {
            long result = jedis.lpush(key, values);
            return result > 0;
        } catch (Exception e) {
            log.error("Error adding values in List:{}", key, e);
            return false;
        }
    }

    public List<String> getValuesInList(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.lrange(key, 0, -1);
        } catch (Exception e) {
            log.error("Error getting values in List:{}", key, e);
            return null;
        }
    }

    public boolean addValuesInSet(String key, String... values) {
        try (Jedis jedis = jedisPool.getResource()) {
            long result = jedis.sadd(key, values);
            return result > 0;
        } catch (Exception e) {
            log.error("Error adding values in Set:{}", key, e);
            return false;
        }
    }

    public List<String> getValuesInSet(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> result = jedis.smembers(key);
            return result.stream().toList();
        } catch (Exception e) {
            log.error("Error getting values in Set:{}", key, e);
            return null;
        }
    }

    public boolean removeValuesInSet(String key, String... values) {
        try (Jedis jedis = jedisPool.getResource()) {
            long result = jedis.srem(key, values);
            return result > 0;
        } catch (Exception e) {
            log.error("Error removing values in Set:{}", key, e);
            return false;
        }
    }
}
