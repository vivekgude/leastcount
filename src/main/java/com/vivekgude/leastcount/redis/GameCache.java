package com.vivekgude.leastcount.redis;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    public static final String CARDS_PER_PLAYER = "cardsPerPlayer";
    public static final String EXIT_SCORE = "exitScore";
    public static final String INVALID_PENALTY = "invalidPenalty";
    public static final String ROUND_NO = "roundNo";
    public static final String MOVE_TIME = "moveTime"; // absolute deadline (epoch millis)
    public static final String MOVE_TIME_CONFIG = "moveTimeConfigMs"; // per-game config override

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
        if (joinedPlayers != null) {
            List<Long> players = joinedPlayers.stream().map(Long::parseLong).collect(Collectors.toList());
//            Collections.reverse(players);
            return players;
        }
        return Collections.emptyList();
    }

    public List<Long> getActivePlayers(String gameId) {
        List<Long> all = getJoinedPlayers(gameId);
        List<Long> eliminated = getEliminatedPlayers(gameId);
        if (eliminated.isEmpty()) return all;
        return all.stream().filter(p -> !eliminated.contains(p)).collect(Collectors.toList());
    }

    public long getCurrentPlayer(String gameId) {
        String currentPlayer = getFieldInMap(GAME + gameId, CURRENT_PLAYER);
        return currentPlayer != null ? Long.parseLong(currentPlayer) : 0;
    }

    public long getMoveTime(String gameId) {
        String moveTime = getFieldInMap(GAME + gameId, MOVE_TIME);
        return moveTime != null ? Long.parseLong(moveTime) : 0;
    }

    public Integer getRoundNoOrNull(String gameId) {
        String v = getFieldInMap(GAME + gameId, ROUND_NO);
        return v != null ? Integer.parseInt(v) : null;
    }

    public void setRoundNo(String gameId, int roundNo) {
        addFieldToMap(GAME + gameId, ROUND_NO, String.valueOf(roundNo));
    }

    // Open pile list operations
    public void setOpenPile(String gameId, List<String> openCards) {
        String key = "open:" + gameId;
        deleteKeys(key);
        if (openCards != null && !openCards.isEmpty()) {
            addValuesInList(key, openCards.toArray(new String[0]));
        }
    }

    public List<String> getOpenPile(String gameId) {
        String key = "open:" + gameId;
        return getValuesInList(key);
    }

    public boolean removeFromOpenPile(String gameId, String card) {
        String key = "open:" + gameId;
        return removeValuesInList(key, card) > 0;
    }

    // Closed deck list operations
    // Semantics:
    // - We store the closed deck as a Redis list and treat the TAIL as the TOP of the deck
    // - We push the deck using LPUSH in the original order so that the first element becomes the tail
    // - We draw using RPOP so draws return in the same order as the Java list provided to setDeck
    public void setDeck(String gameId, List<String> deck) {
        String key = "deck:" + gameId;
        deleteKeys(key);
        if (deck != null && !deck.isEmpty()) {
            addValuesInList(key, deck.toArray(new String[0]));
        }
    }

    public String popFromDeck(String gameId) {
        String key = "deck:" + gameId;
        return popFromListTail(key);
    }

    public int getDeckCount(String gameId) {
        String key = "deck:" + gameId;
        return getListSize(key);
    }

    // Cumulative game score operations
    public int getGameScore(String gameId, long playerId) {
        String key = "gameScore:" + gameId;
        String v = getFieldInMap(key, String.valueOf(playerId));
        return v != null ? Integer.parseInt(v) : 0;
    }

    public void setGameScore(String gameId, long playerId, int score) {
        String key = "gameScore:" + gameId;
        addFieldToMap(key, String.valueOf(playerId), String.valueOf(score));
    }

    public Map<String, String> getAllGameScores(String gameId) {
        String key = "gameScore:" + gameId;
        return getFieldsInMap(key);
    }

    // Eliminated players operations
    public void addEliminated(String gameId, long playerId) {
        addValuesInSet("eliminated:" + gameId, String.valueOf(playerId));
    }

    public List<Long> getEliminatedPlayers(String gameId) {
        List<String> vals = getValuesInSet("eliminated:" + gameId);
        if (vals == null) return Collections.emptyList();
        List<Long> out = new ArrayList<>();
        for (String v : vals) out.add(Long.parseLong(v));
        return out;
    }

    public Integer getCardsPerPlayerOrNull(String gameId) {
        String v = getFieldInMap(GAME + gameId, CARDS_PER_PLAYER);
        return v != null ? Integer.parseInt(v) : null;
    }

    public Integer getExitScoreOrNull(String gameId) {
        String v = getFieldInMap(GAME + gameId, EXIT_SCORE);
        return v != null ? Integer.parseInt(v) : null;
    }

    public Integer getInvalidPenaltyOrNull(String gameId) {
        String v = getFieldInMap(GAME + gameId, INVALID_PENALTY);
        return v != null ? Integer.parseInt(v) : null;
    }

    public Long getMoveTimeConfigOrNull(String gameId) {
        String v = getFieldInMap(GAME + gameId, MOVE_TIME_CONFIG);
        try {
            return v != null ? Long.parseLong(v) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
