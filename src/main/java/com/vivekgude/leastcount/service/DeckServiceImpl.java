package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.Utils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DeckServiceImpl implements DeckService {

    private final GameCache gameCache;
    private final PlayerCache playerCache;

    public DeckServiceImpl(GameCache gameCache, PlayerCache playerCache) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
    }

    @Override
    public String drawFromClosed(String gameId) {
        String card = gameCache.popFromDeck(gameId);
        if (card != null) return card;

        rebuildClosed(gameId);
        return gameCache.popFromDeck(gameId);
    }

    @Override
    public int getClosedCount(String gameId) {
        return gameCache.getDeckCount(gameId);
    }

    private void rebuildClosed(String gameId) {
        // Build full two-deck set
        List<String> full = Utils.generateShuffledDecks(2);
        Set<String> exclude = new HashSet<>();
        // Exclude players' hands
        List<Long> players = gameCache.getJoinedPlayers(gameId);
        for (Long pid : players) {
            List<String> hand = playerCache.getPlayerCards(gameId, String.valueOf(pid));
            if (hand != null) exclude.addAll(hand);
        }
        // Exclude current open pile
        List<String> open = gameCache.getOpenPile(gameId);
        if (open != null) exclude.addAll(open);

        List<String> remainder = new ArrayList<>();
        for (String c : full) {
            if (!exclude.contains(c)) remainder.add(c);
        }
        gameCache.setDeck(gameId, remainder);
    }
}


