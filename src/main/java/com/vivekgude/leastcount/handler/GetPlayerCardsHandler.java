package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.model.ws.response.CardsRes;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class GetPlayerCardsHandler implements MessageHandler {

    private final GameCache gameCache;
    private final PlayerCache playerCache;

    public GetPlayerCardsHandler(GameCache gameCache, PlayerCache playerCache) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
    }

    @Override
    public void handleMessage(WebSocketReq message) {
        try {
            String gameId = message.getGameId();
            long userId = message.getUserId();

            if (gameId == null || gameId.trim().isEmpty()) {
                log.error("Get player cards request rejected: Invalid game ID");
                return;
            }

            // Check if game exists and is in progress
            int gameState = gameCache.getGameState(gameId);
            if (gameState == GameState.INVALID.getType()) {
                log.error("Get player cards request rejected: Game {} not found", gameId);
                return;
            }

            // Check if user is part of the game
            List<Long> players = gameCache.getJoinedPlayers(gameId);
            if (!players.contains(userId)) {
                log.debug("Get player cards request rejected: User {} is not part of game {}", userId, gameId);
                return;
            }

            // Get player's cards
            List<String> playerCards = playerCache.getPlayerCards(gameId, String.valueOf(userId));
            if (playerCards == null || playerCards.isEmpty()) {
                log.debug("No cards found for player {} in game {}", userId, gameId);
                return;
            }

            // Send cards to the player
            CardsRes cardsRes = new CardsRes();
            cardsRes.setType("cardsres");
            cardsRes.setGameId(gameId);
            cardsRes.setCards(playerCards);
            cardsRes.setTotalCards(playerCards.size());
            cardsRes.setReceiver(userId);

            WebSocketUtil.sendMessage(gameId, userId, cardsRes);
            log.info("Sent {} cards to player {} in game {}", playerCards.size(), userId, gameId);

        } catch (Exception e) {
            log.error("Error getting player cards: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getMessageType() {
        return "playercardsreq";
    }
} 