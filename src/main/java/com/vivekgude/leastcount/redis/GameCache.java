package com.vivekgude.leastcount.redis;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GameCache extends BaseCache {

    public static final String STATE = "state";
    public static final String HOST = "host";
    public static final String HOST_NAME = "host_name";

    public Map<String, String> getGameDetails(String gameId) {
        String gameIdKey = GAME + gameId;
        return getFieldsInMap(gameIdKey);
    }

    public void addGameDetail(String gameId, String gameDetail, String value) {
        String gameIdKey = GAME + gameId;
        addFieldToMap(gameIdKey, gameDetail, value);
    }

    public int getGameState(String gameId) {
        String gameState = getFieldInMap(GAME + gameId, STATE);
        return gameState != null ? Integer.parseInt(gameState) : 0;
    }

    public List<Long> getJoinedPlayers(String gameId) {
        List<String> joinedPlayers = getValuesInSet(JOINED_PLAYERS + gameId);
        return joinedPlayers != null ?
                joinedPlayers.stream().map(Long::parseLong).collect(Collectors.toList()) :
                Collections.emptyList();
    }

}
