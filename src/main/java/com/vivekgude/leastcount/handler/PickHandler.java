package com.vivekgude.leastcount.handler;

import com.google.gson.Gson;
import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.job.JobSchedulerService;
import com.vivekgude.leastcount.job.TurnTimerJob;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.model.ws.response.CardsRes;
import com.vivekgude.leastcount.model.ws.response.PickRes;
import com.vivekgude.leastcount.model.ws.response.StateUpdate;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.service.DeckService;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.vivekgude.leastcount.constants.Constants.DEFAULT_MOVE_TIME_MS;
import static com.vivekgude.leastcount.redis.GameCache.*;

@Slf4j
@Component
public class PickHandler extends AbstractMessageHandler implements MessageHandler {

    private final GameCache gameCache;
    private final PlayerCache playerCache;
    private final DeckService deckService;
    private final JobSchedulerService jobSchedulerService;
    private final Gson gson = new Gson();

    public PickHandler(GameCache gameCache, PlayerCache playerCache, DeckService deckService,
                       JobSchedulerService jobSchedulerService) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
        this.deckService = deckService;
        this.jobSchedulerService = jobSchedulerService;
    }

    @Override
    public void handleMessage(WebSocketReq message) {
        try {
            String gameId = message.getGameId();
            long userId = message.getUserId();
            if (gameId == null || gameId.isBlank()) return;

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

            // Enforce drop-before-pick
            String lastAction = gameCache.getFieldInMap(GAME + gameId, "lastAction");
            String lastActor = gameCache.getFieldInMap(GAME + gameId, "lastActor");
            if (!"DROP".equals(lastAction) || !String.valueOf(userId).equals(lastActor)) {
                sendError(gameId, userId, "drop_first", "You must drop before picking");
                return;
            }

            PickPayload payload = gson.fromJson(message.getContent(), PickPayload.class);
            if (payload == null || payload.source == null) {
                sendError(gameId, userId, "invalid_payload", "Missing source");
                return;
            }

            String pickedCard = null;
            if ("open".equalsIgnoreCase(payload.source)) {
                if (payload.card == null) {
                    sendError(gameId, userId, "invalid_payload", "Missing card for open pick");
                    return;
                }
                // remove specific card from open pile
                boolean removed = gameCache.removeFromOpenPile(gameId, payload.card);
                if (!removed) {
                    sendError(gameId, userId, "invalid_open_card", "Card not in open pile");
                    return;
                }
                pickedCard = payload.card;
            } else if ("closed".equalsIgnoreCase(payload.source)) {
                pickedCard = deckService.drawFromClosed(gameId);
                if (pickedCard == null) {
                    sendError(gameId, userId, "deck_empty", "Closed deck empty");
                    return;
                }
            } else {
                sendError(gameId, userId, "invalid_source", "Source must be open or closed");
                return;
            }

            // if picking from open, ensure exactly one card provided
            if ("open".equalsIgnoreCase(payload.source) && (payload.card == null || payload.card.isBlank())) {
                return;
            }

            // Add to player's hand
            List<String> hand = new ArrayList<>(playerCache.getPlayerCards(gameId, String.valueOf(userId)));
            hand.add(pickedCard);
            playerCache.setPlayerCards(gameId, String.valueOf(userId), hand);

            // Advance turn (skip eliminated players)
            List<Long> players = gameCache.getActivePlayers(gameId);
            int currentIndex = players.indexOf(current);
            long nextPlayer = players.get((currentIndex + 1) % players.size());
            gameCache.addFieldToMap(GAME + gameId, CURRENT_PLAYER, String.valueOf(nextPlayer));

            long configuredMove = Optional.ofNullable(gameCache.getMoveTimeConfigOrNull(gameId))
                    .orElse(DEFAULT_MOVE_TIME_MS);
            long newMoveTime = System.currentTimeMillis() + configuredMove;
            gameCache.addFieldToMap(GAME + gameId, MOVE_TIME, String.valueOf(newMoveTime));

            // Clear turn marker
            gameCache.addFieldToMap(GAME + gameId, "lastAction", "NONE");
            gameCache.addFieldToMap(GAME + gameId, "lastActor", "0");

            // Schedule next timer
            try {
                jobSchedulerService.deleteJob("turnTimer_" + gameId);
                Map<String, Object> jobData = new HashMap<>();
                jobData.put("gameId", gameId);
                jobSchedulerService.scheduleOneTimeJob("turnTimer_" + gameId, TurnTimerJob.class,
                        new Date(newMoveTime), jobData);
            } catch (Exception ex) {
                log.error("Failed to schedule next timer for game {}", gameId, ex);
            }

            // Broadcast pick response and state update
            PickRes pickRes = new PickRes();
            pickRes.setType("pickres");
            pickRes.setPlayerId(userId);
            pickRes.setSource(payload.source);
            pickRes.setCard("open".equalsIgnoreCase(payload.source) ? pickedCard : "");
            pickRes.setOpen(Optional.ofNullable(gameCache.getOpenPile(gameId)).orElse(List.of()));
            WebSocketUtil.broadcastToGame(gameId, pickRes);

            // send updated cards privately to the picker
            try {
                CardsRes cardsRes = new CardsRes();
                cardsRes.setType("cardsres");
                cardsRes.setGameId(gameId);
                cardsRes.setCards(hand);
                cardsRes.setReceiver(userId);
                WebSocketUtil.sendMessage(gameId, userId, cardsRes);
            } catch (Exception ignore) {}

            StateUpdate state = new StateUpdate();
            state.setType("stateupdate");
            state.setCurrentPlayer(nextPlayer);
            state.setMoveTime(newMoveTime);
            state.setOpen(Optional.ofNullable(gameCache.getOpenPile(gameId)).orElse(List.of()));
            state.setDeckCount(deckService.getClosedCount(gameId));
            WebSocketUtil.broadcastToGame(gameId, state);

        } catch (Exception e) {
            log.error("PickHandler error: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getMessageType() {
        return "pickreq";
    }

    private static class PickPayload {
        String source;
        String card;
    }
}


