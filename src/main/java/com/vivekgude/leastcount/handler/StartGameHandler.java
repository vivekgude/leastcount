package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.job.InitialPlayerMoveJob;
import com.vivekgude.leastcount.job.JobSchedulerService;
import com.vivekgude.leastcount.job.TurnTimerJob;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.model.ws.response.CardsRes;
import com.vivekgude.leastcount.model.ws.response.GameStartRes;
import com.vivekgude.leastcount.model.ws.response.Score;
import com.vivekgude.leastcount.model.ws.response.ScoreRes;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.Utils;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

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

            if (gameId == null || gameId.trim().isEmpty()) {
                log.error("Game start request rejected: Invalid game ID");
                return;
            }

            // Check if user is host and game is in waiting state
            String hostId = gameCache.getFieldInMap(GAME + gameId, HOST);
            int gameState = gameCache.getGameState(gameId);

//            if (hostId == null) {
//                log.error("Game start request rejected: Game {} not found", gameId);
//                return;
//            }
//
//            if (!String.valueOf(userId).equals(hostId)) {
//                log.warn("Game start request rejected: User {} is not host of game {}", userId, gameId);
//                return;
//            }

            if (gameState != GameState.WAITING.getType()) {
                log.debug("Game start request rejected: Game {} is not in waiting state. Current state: {}", gameId, gameState);
                return;
            }

            // Get all players in the game
            List<Long> players = gameCache.getJoinedPlayers(gameId);

            // Generate and distribute cards
            List<String> deck = Utils.generateShuffledDecks(DECK_SIZE);
            log.info("Generated deck for game {}: {} cards", gameId, deck.size());
            
            // Distribute cards to each player
            int cardsPerPlayer = CARDS_PER_PLAYER;
            log.info("Distributing {} cards per player for game {}", cardsPerPlayer, gameId);
            
            for (int i = 0; i < players.size(); i++) {
                long playerId = players.get(i);
                int startIndex = i * cardsPerPlayer;
                int endIndex = startIndex + cardsPerPlayer;
                List<String> playerCards = deck.subList(startIndex, endIndex);
                
                // Store cards in cache
                playerCache.setPlayerCards(gameId, String.valueOf(playerId), playerCards);
                
                // Initialize player score to 0
                playerCache.initializePlayerScore(gameId, String.valueOf(playerId));

                log.info("Assigned {} cards to player {} in game {} and initialized score to 0", playerCards,
                        playerId, gameId);
            }

            // Update game state to INPROGRESS
            gameCache.addFieldToMap(GAME + gameId, STATE, String.valueOf(GameState.INPROGRESS.getType()));
            
            // Set first player's turn
            long firstPlayer = players.get(0);
            gameCache.addFieldToMap(GAME + gameId, CURRENT_PLAYER, String.valueOf(firstPlayer));
            
            // Set move time (30 seconds)
            long moveTime = System.currentTimeMillis() + MOVE_TIME_MS;
            gameCache.addFieldToMap(GAME + gameId, MOVE_TIME, String.valueOf(moveTime));

            // Send game start response to all players
            GameStartRes gameStartRes = new GameStartRes(GameState.INPROGRESS.getType(), firstPlayer, moveTime);
            gameStartRes.setType("gamestartres");
            WebSocketUtil.broadcastToGame(gameId, gameStartRes);

            List<Score> scores = new ArrayList<>();

            // Send individual card responses to each player
            for (Long playerId : players) {
                List<String> playerCards = playerCache.getPlayerCards(gameId, String.valueOf(playerId));
                if (playerCards != null && !playerCards.isEmpty()) {
                    CardsRes cardsRes = new CardsRes();
                    cardsRes.setType("cardsres");
                    cardsRes.setGameId(gameId);
                    cardsRes.setCards(playerCards);
                    cardsRes.setReceiver(playerId);
                    
                    // Send cards to this specific player
                    WebSocketUtil.sendMessage(gameId, playerId, cardsRes);
                    log.info("Sent {} cards to player {} in game {}", playerCards.size(), playerId, gameId);
                    scores.add(new Score(playerId, 0));
                } else {
                    log.error("Failed to retrieve cards for player {} in game {}", playerId, gameId);
                }
            }

            ScoreRes scoreRes = new ScoreRes(scores);
            WebSocketUtil.broadcastToGame(gameId, scoreRes);

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

            log.info("Game {} started successfully with {} players", gameId, players.size());

        } catch (Exception e) {
            log.error("Error starting game: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getMessageType() {
        return "startgamereq";
    }
} 