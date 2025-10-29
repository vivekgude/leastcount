package com.vivekgude.leastcount.job;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.ws.response.CardsRes;
import com.vivekgude.leastcount.model.ws.response.GameStartRes;
import com.vivekgude.leastcount.model.ws.response.StateUpdate;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.service.DeckService;
import com.vivekgude.leastcount.util.Utils;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.vivekgude.leastcount.constants.Constants.DEFAULT_CARDS_PER_PLAYER;
import static com.vivekgude.leastcount.constants.Constants.DEFAULT_MOVE_TIME_MS;
import static com.vivekgude.leastcount.redis.GameCache.*;

@Slf4j
@Component
public class NextRoundJob extends BaseJob {

    private final GameCache gameCache;
    private final PlayerCache playerCache;
    private final DeckService deckService;
    private final JobSchedulerService jobSchedulerService;

    public NextRoundJob(GameCache gameCache, PlayerCache playerCache, DeckService deckService,
                       JobSchedulerService jobSchedulerService) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
        this.deckService = deckService;
        this.jobSchedulerService = jobSchedulerService;
    }

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        String gameId = context.getJobDetail().getJobDataMap().getString("gameId");
        
        // Check if game is still in waiting state
        int gameState = gameCache.getGameState(gameId);
        if (gameState != GameState.WAITING.getType()) {
            log.warn("NextRoundJob: Game {} is not in waiting state, current: {}", gameId, gameState);
            return;
        }

        // Get active players (non-eliminated)
        List<Long> activePlayers = gameCache.getActivePlayers(gameId);
        if (activePlayers.size() < 2) {
            log.warn("NextRoundJob: Not enough active players for game {}", gameId);
            return;
        }

        // Get per-game configuration
        Integer cardsPerPlayer = gameCache.getCardsPerPlayerOrNull(gameId);
        if (cardsPerPlayer == null) {
            cardsPerPlayer = DEFAULT_CARDS_PER_PLAYER;
        }

        // Generate new deck and deal cards
        List<String> deck = Utils.generateTwoDecksShuffled();
        List<String> remainingDeck = new ArrayList<>(deck);

        // Deal cards to active players
        for (Long playerId : activePlayers) {
            List<String> playerCards = new ArrayList<>();
            for (int i = 0; i < cardsPerPlayer; i++) {
                if (!remainingDeck.isEmpty()) {
                    playerCards.add(remainingDeck.remove(0));
                }
            }
            playerCache.setPlayerCards(gameId, String.valueOf(playerId), playerCards);
        }

        // Set up game state
        gameCache.setDeck(gameId, remainingDeck);
        gameCache.setOpenPile(gameId, Collections.emptyList());
        gameCache.addFieldToMap(GAME + gameId, STATE, String.valueOf(GameState.INPROGRESS.getType()));

        // Set first player and move time
        Long firstPlayer = activePlayers.get(0);
        gameCache.addFieldToMap(GAME + gameId, CURRENT_PLAYER, String.valueOf(firstPlayer));

        Long moveTimeConfig = gameCache.getMoveTimeConfigOrNull(gameId);
        long moveTime = System.currentTimeMillis() + (moveTimeConfig != null ? moveTimeConfig : DEFAULT_MOVE_TIME_MS);
        gameCache.addFieldToMap(GAME + gameId, MOVE_TIME, String.valueOf(moveTime));

        // Broadcast game start
        GameStartRes gameStartRes = new GameStartRes();
        gameStartRes.setType("gamestartres");
        gameStartRes.setGameId(gameId);
        gameStartRes.setPlayers(activePlayers);
        WebSocketUtil.broadcastToGame(gameId, gameStartRes);

        // Send cards to each player privately
        for (Long playerId : activePlayers) {
            List<String> playerCards = playerCache.getPlayerCards(gameId, String.valueOf(playerId));
            CardsRes cardsRes = new CardsRes();
            cardsRes.setType("cardsres");
            cardsRes.setGameId(gameId);
            cardsRes.setCards(playerCards);
            cardsRes.setReceiver(playerId);
            WebSocketUtil.sendMessage(gameId, playerId, cardsRes);
        }

        // Broadcast state update
        StateUpdate state = new StateUpdate();
        state.setType("stateupdate");
        state.setCurrentPlayer(firstPlayer);
        state.setMoveTime(moveTime);
        state.setOpen(Collections.emptyList());
        state.setDeckCount(remainingDeck.size());
        state.setGameScores(gameCache.getAllGameScores(gameId));
        state.setEliminated(gameCache.getEliminatedPlayers(gameId));
        Integer roundNo = gameCache.getRoundNoOrNull(gameId);
        state.setRoundNo(roundNo != null ? roundNo : 1);
        WebSocketUtil.broadcastToGame(gameId, state);

        // Schedule turn timer
        try {
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("gameId", gameId);
            jobSchedulerService.scheduleOneTimeJob("turnTimer_" + gameId, TurnTimerJob.class,
                    new Date(moveTime), jobData);
        } catch (Exception e) {
            log.error("Failed to schedule turn timer for game {}", gameId, e);
        }

        log.info("NextRoundJob: Started round for game {} with {} active players", gameId, activePlayers.size());
    }
}