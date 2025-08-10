package com.vivekgude.leastcount.handler;

import com.google.gson.Gson;
import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.model.ws.response.DropRes;
import com.vivekgude.leastcount.model.ws.response.StateUpdate;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.vivekgude.leastcount.redis.GameCache.*;

@Slf4j
@Component
public class DropHandler extends AbstractMessageHandler implements MessageHandler {

    private final GameCache gameCache;
    private final PlayerCache playerCache;
    private final Gson gson = new Gson();

    public DropHandler(GameCache gameCache, PlayerCache playerCache) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
    }

    @Override
    public void handleMessage(WebSocketReq message) {
        try {
            String gameId = message.getGameId();
            long userId = message.getUserId();
            if (gameId == null || gameId.isBlank())
                return;

            if (gameCache.getGameState(gameId) != GameState.INPROGRESS.getType()) {
                sendError(gameId, userId, "invalid_state", "Game not in progress");
                return;
            }

            // reject eliminated players
            if (gameCache.getEliminatedPlayers(gameId).contains(userId)) {
                sendError(gameId, userId, "eliminated", "You are eliminated");
                return;
            }

            long current = gameCache.getCurrentPlayer(gameId);
            if (current != userId) {
                sendError(gameId, userId, "not_your_turn", "Not your turn");
                return;
            }

            // Parse content: { "cards": ["7h", "7s"] }
            DropPayload payload = gson.fromJson(message.getContent(), DropPayload.class);
            if (payload == null || payload.cards == null || payload.cards.isEmpty()) {
                sendError(gameId, userId, "invalid_payload", "No cards provided");
                return;
            }

            // Validate ownership and same-rank
            List<String> hand = new ArrayList<>(playerCache.getPlayerCards(gameId, String.valueOf(userId)));
            if (!hand.containsAll(payload.cards)) {
                sendError(gameId, userId, "ownership", "You don't own all cards");
                return;
            }
            if (!allSameRank(payload.cards)) {
                sendError(gameId, userId, "invalid_drop", "All dropped cards must be same rank");
                return;
            }

            // enforce at least one card dropped
            if (payload.cards.isEmpty()) return;

            // Remove from hand and set open pile
            hand.removeAll(payload.cards);
            playerCache.setPlayerCards(gameId, String.valueOf(userId), hand);
            gameCache.setOpenPile(gameId, payload.cards);

            // Mark last action as DROP
            gameCache.addFieldToMap(GAME + gameId, "lastAction", "DROP");
            gameCache.addFieldToMap(GAME + gameId, "lastActor", String.valueOf(userId));

            // Broadcast typed DropRes
            DropRes dropRes = new DropRes();
            dropRes.setType("dropres");
            dropRes.setPlayerId(userId);
            dropRes.setOpen(payload.cards);
            dropRes.setDeckCount(gameCache.getDeckCount(gameId));
            WebSocketUtil.broadcastToGame(gameId, dropRes);

            // Also echo updated state snapshot
            StateUpdate state = new StateUpdate();
            state.setType("stateupdate");
            state.setCurrentPlayer(userId);
            state.setMoveTime(gameCache.getMoveTime(gameId));
            state.setOpen(gameCache.getOpenPile(gameId));
            state.setDeckCount(gameCache.getDeckCount(gameId));
            WebSocketUtil.broadcastToGame(gameId, state);
        } catch (Exception e) {
            log.error("DropHandler error: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getMessageType() {
        return "dropreq";
    }

    private boolean allSameRank(List<String> cards) {
        String first = cards.get(0);
        String rank = first.substring(0, first.length() - 1);
        for (int i = 1; i < cards.size(); i++) {
            String r = cards.get(i).substring(0, cards.get(i).length() - 1);
            if (!rank.equals(r))
                return false;
        }
        return true;
    }

    private static class DropPayload {
        List<String> cards;
    }
}


