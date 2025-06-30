package com.vivekgude.leastcount.redis;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlayerCache extends BaseCache {

    public static final String PLAYER_NAME = "playerName";
    public static final String PLAYER_CARDS = "playerCards";
    public static final String PLAYER_SCORE = "playerScore";

    public void setPlayerCards(String gameId, String playerId, List<String> playerCards) {
        String playerGameIdKey = PLAYER + gameId + ":" + playerId + PLAYER_CARDS;
        // Clear existing cards first
        deleteKeys(playerGameIdKey);
        // Add new cards
        if (playerCards != null && !playerCards.isEmpty()) {
            addValuesInList(playerGameIdKey, playerCards.toArray(new String[0]));
        }
    }

    public List<String> getPlayerCards(String gameId, String playerId) {
        String playerGameIdKey = PLAYER + gameId + ":" + playerId + PLAYER_CARDS;
        return getValuesInList(playerGameIdKey);
    }

    public void removePlayerCards(String gameId, String playerId) {
        String playerGameIdKey = PLAYER + gameId + ":" + playerId + PLAYER_CARDS;
        deleteKeys(playerGameIdKey);
    }

    public String getPlayerName(String gameId, long playerId) {
        String playerKey = PLAYER + playerId + ":" + GAME + gameId;
        return getFieldInMap(playerKey, PLAYER_NAME);
    }

    public void addPlayerName(String gameId, long playerId, String playerName) {
        String playerKey = PLAYER + playerId + ":" + GAME + gameId;
        addFieldToMap(playerKey, PLAYER_NAME, playerName);
    }

    /**
     * Initialize player score to 0 when game starts
     */
    public void initializePlayerScore(String gameId, String playerId) {
        String playerKey = PLAYER + playerId + ":" + GAME + gameId;
        addFieldToMap(playerKey, PLAYER_SCORE, "0");
    }

    /**
     * Get player's current score
     */
    public int getPlayerScore(String gameId, String playerId) {
        String playerKey = PLAYER + playerId + ":" + GAME + gameId;
        String score = getFieldInMap(playerKey, PLAYER_SCORE);
        return score != null ? Integer.parseInt(score) : 0;
    }

    /**
     * Update player's score
     */
    public void updatePlayerScore(String gameId, String playerId, int newScore) {
        String playerKey = PLAYER + playerId + ":" + GAME + gameId;
        addFieldToMap(playerKey, PLAYER_SCORE, String.valueOf(newScore));
    }

}
