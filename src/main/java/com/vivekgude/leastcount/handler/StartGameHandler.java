package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.job.InitialPlayerMoveJob;
import com.vivekgude.leastcount.job.JobSchedulerService;
import com.vivekgude.leastcount.job.TurnTimerJob;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.model.ws.response.GameStartRes;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.Utils;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vivekgude.leastcount.constants.Constants.*;
import static com.vivekgude.leastcount.redis.GameCache.*;

@Slf4j
@Component
public class StartGameHandler implements MessageHandler {

    private final GameCache gameCache;
    private final PlayerCache playerCache;
    private final JobSchedulerService jobSchedulerService;

    public StartGameHandler(GameCache gameCache, PlayerCache playerCache,
            JobSchedulerService jobSchedulerService) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
        this.jobSchedulerService = jobSchedulerService;
    }

    @Override
    public void handleMessage(WebSocketReq message) {
        try {
            String gameId = message.getGameId();
            long userId = message.getUserId();

            // Check if user is host and game is in waiting state
            String hostId = gameCache.getFieldInMap(GAME + gameId, HOST);
            int gameState = gameCache.getGameState(gameId);

            if (!String.valueOf(userId).equals(hostId) || gameState != GameState.WAITING.getType()) {
                log.warn("Game start request rejected. User: {}, Game: {}, State: {}", userId, gameId,
                        gameState);
                return;
            }

            // Generate and distribute cards
            List<String> deck = Utils.generateShuffledDecks(DECK_SIZE);
            List<Long> players = gameCache.getJoinedPlayers(gameId);
            
            // Distribute cards to players
            int cardsPerPlayer = deck.size() / players.size();
            for (int i = 0; i < players.size(); i++) {
                List<String> playerCards = deck.subList(i * cardsPerPlayer, (i + 1) * cardsPerPlayer);
                playerCache.setPlayerCards(gameId, String.valueOf(players.get(i)), playerCards);
            }

            // Update game state
            gameCache.addFieldToMap(GAME + gameId, STATE, String.valueOf(GameState.INPROGRESS.getType()));
            
            // Set first player's turn
            long firstPlayer = players.get(0);
            gameCache.addFieldToMap(GAME + gameId, CURRENT_PLAYER, String.valueOf(firstPlayer));
            
            // Set move time (30 seconds)
            long moveTime = System.currentTimeMillis() + MOVE_TIME_MS;
            gameCache.addFieldToMap(GAME + gameId, MOVE_TIME, String.valueOf(moveTime));

            // Send game start response immediately
            GameStartRes gameStartRes = new GameStartRes(GameState.INPROGRESS.getType(), firstPlayer, moveTime);
            WebSocketUtil.broadcastToGame(gameId, gameStartRes);

            // Schedule turn timer job
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("gameId", gameId);
            jobSchedulerService.scheduleOneTimeJob("turnTimer_" + gameId, TurnTimerJob.class,
                    new Date(moveTime), jobData);

            // Schedule initial player move notification after 5 seconds
            Map<String, Object> initialMoveData = new HashMap<>();
            initialMoveData.put("gameId", gameId);
            jobSchedulerService.scheduleOneTimeJob("initialPlayerMove_" + gameId, InitialPlayerMoveJob.class,
                    new Date(System.currentTimeMillis() + 5000), initialMoveData);

        } catch (Exception e) {
            log.error("Error starting game: {}", e.getMessage());
        }
    }

    @Override
    public String getMessageType() {
        return "startgamereq";
    }
} 