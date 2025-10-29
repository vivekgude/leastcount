package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.Utils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeckService {

    private final GameCache gameCache;
    private final PlayerCache playerCache;

    public DeckService(GameCache gameCache, PlayerCache playerCache) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
    }

    /**
     * Rebuild the closed pile by reshuffling all cards not in players' hands or open pile
     */
    public void rebuildClosedPile(String gameId) {
        // Get the full two-deck set (104 cards)
        List<String> fullDeck = Utils.generateTwoDecksShuffled();
        List<String> availableCards = new ArrayList<>(fullDeck);
        
        // Remove cards that are in players' hands (accounting for duplicates in two-deck game)
        List<Long> activePlayers = gameCache.getActivePlayers(gameId);
        for (Long playerId : activePlayers) {
            List<String> playerCards = playerCache.getPlayerCards(gameId, String.valueOf(playerId));
            if (playerCards != null) {
                // Remove each card from availableCards (handles duplicates correctly)
                for (String card : playerCards) {
                    availableCards.remove(card); // remove() removes first occurrence, which is correct for counting
                }
            }
        }
        
        // Remove cards that are in the open pile (accounting for duplicates)
        List<String> openPile = gameCache.getOpenPile(gameId);
        if (openPile != null) {
            for (String card : openPile) {
                availableCards.remove(card);
            }
        }
        
        // Shuffle the remaining cards and set as the new closed pile
        java.util.Collections.shuffle(availableCards);
        gameCache.setDeck(gameId, availableCards);
    }

    /**
     * Draw a card from the closed pile, rebuilding if necessary
     */
    public String drawFromClosedPile(String gameId) {
        String card = gameCache.popFromDeck(gameId);
        
        // If deck is empty, rebuild it
        if (card == null || gameCache.getDeckCount(gameId) == 0) {
            rebuildClosedPile(gameId);
            card = gameCache.popFromDeck(gameId);
        }
        
        return card;
    }
}