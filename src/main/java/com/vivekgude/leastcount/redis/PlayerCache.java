package com.vivekgude.leastcount.redis;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlayerCache extends BaseCache {

    public static final String PLAYER_CARDS = "playerCards";

    public void setPlayerCards(String gameId, String playerId, List<String> playerCards) {
        String playerGameIdKey = PLAYER + gameId + ":" + playerId + PLAYER_CARDS;
        addValuesInList(playerGameIdKey, playerCards.toArray(new String[0]));
    }

    public List<String> getPlayerCards(String gameId, String playerId) {
        String playerGameIdKey = PLAYER + gameId + ":" + playerId + PLAYER_CARDS;
        return getValuesInList(playerGameIdKey);
    }

}
