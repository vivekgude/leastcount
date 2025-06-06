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
    public static final String HOST_NAME = "hostName";
    public static final String CURRENT_PLAYER = "currentPlayer";
    public static final String MOVE_TIME = "moveTime";

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

    public long getCurrentPlayer(String gameId) {
        String currentPlayer = getFieldInMap(GAME + gameId, CURRENT_PLAYER);
        return currentPlayer != null ? Long.parseLong(currentPlayer) : 0;
    }

    public long getMoveTime(String gameId) {
        String moveTime = getFieldInMap(GAME + gameId, MOVE_TIME);
        return moveTime != null ? Long.parseLong(moveTime) : 0;
    }
}
