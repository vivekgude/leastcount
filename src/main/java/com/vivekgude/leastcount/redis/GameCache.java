package com.vivekgude.leastcount.redis;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GameCache extends BaseCache {

    public static final String STATE = "state";
    public static final String PLAYERS = "players";

    public Map<String, String> getGameDetails(String gameId) {
        String gameIdKey = GAME + gameId;
        return getFieldsInSet(gameIdKey);
    }

    public void addGameDetail(String gameId, String gameDetail, String value) {
        String gameIdKey = GAME + gameId;
        addFieldToSet(gameIdKey, gameDetail, value);
    }

}
