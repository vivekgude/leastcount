package com.vivekgude.leastcount.job;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.ws.response.CardsRes;
import com.vivekgude.leastcount.model.ws.response.GameStartRes;
import com.vivekgude.leastcount.model.ws.response.Score;
import com.vivekgude.leastcount.model.ws.response.ScoreRes;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.Utils;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.vivekgude.leastcount.constants.Constants.DEFAULT_CARDS_PER_PLAYER;
import static com.vivekgude.leastcount.constants.Constants.DEFAULT_MOVE_TIME_MS;
import static com.vivekgude.leastcount.constants.Constants.DECK_SIZE;
import static com.vivekgude.leastcount.redis.GameCache.*;

@Slf4j
@Component
public class NextRoundJob extends BaseJob {

    private final GameCache gameCache;
    private final PlayerCache playerCache;
    private final JobSchedulerService jobSchedulerService;

    public NextRoundJob(GameCache gameCache, PlayerCache playerCache, JobSchedulerService jobSchedulerService) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
        this.jobSchedulerService = jobSchedulerService;
    }

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        String gameId = context.getJobDetail().getJobDataMap().getString("gameId");

        // Validate we are in WAITING state and enough active players
        if (gameCache.getGameState(gameId) != GameState.WAITING.getType()) {
            return;
        }
        List<Long> players = gameCache.getActivePlayers(gameId);
        if (players == null || players.size() < 2) {
            return;
        }

        // Build two-deck shuffled and deal
        List<String> deck = Utils.generateShuffledDecks(DECK_SIZE);
        int cardsPerPlayer = Optional.ofNullable(gameCache.getCardsPerPlayerOrNull(gameId))
                .orElse(DEFAULT_CARDS_PER_PLAYER);

        int dealt = 0;
        for (Long pid : players) {
            int startIndex = dealt;
            int endIndex = startIndex + cardsPerPlayer;
            List<String> playerCards = new ArrayList<>(deck.subList(startIndex, endIndex));
            dealt += cardsPerPlayer;
            playerCache.setPlayerCards(gameId, String.valueOf(pid), playerCards);
        }

        // Initialize piles
        gameCache.setOpenPile(gameId, Collections.emptyList());
        if (dealt < deck.size()) {
            gameCache.setDeck(gameId, new ArrayList<>(deck.subList(dealt, deck.size())));
        } else {
            gameCache.setDeck(gameId, Collections.emptyList());
        }

        // Set INPROGRESS, first player, and move time
        gameCache.addFieldToMap(GAME + gameId, STATE, String.valueOf(GameState.INPROGRESS.getType()));
        long firstPlayer = players.get(0);
        gameCache.addFieldToMap(GAME + gameId, CURRENT_PLAYER, String.valueOf(firstPlayer));
        long configuredMove = Optional.ofNullable(gameCache.getMoveTimeConfigOrNull(gameId))
                .orElse(DEFAULT_MOVE_TIME_MS);
        long moveTime = System.currentTimeMillis() + configuredMove;
        gameCache.addFieldToMap(GAME + gameId, MOVE_TIME, String.valueOf(moveTime));

        // Broadcast gamestart and private hands
        GameStartRes startRes = new GameStartRes(GameState.INPROGRESS.getType(), firstPlayer, moveTime);
        startRes.setType("gamestartres");
        WebSocketUtil.broadcastToGame(gameId, startRes);

        for (Long pid : players) {
            List<String> playerCards = playerCache.getPlayerCards(gameId, String.valueOf(pid));
            if (playerCards != null && !playerCards.isEmpty()) {
                CardsRes cardsRes = new CardsRes();
                cardsRes.setType("cardsres");
                cardsRes.setGameId(gameId);
                cardsRes.setCards(playerCards);
                cardsRes.setReceiver(pid);
                WebSocketUtil.sendMessage(gameId, pid, cardsRes);
            }
        }

        // Broadcast current cumulative scores
        List<Score> scores = new ArrayList<>();
        for (Long pid : gameCache.getJoinedPlayers(gameId)) {
            scores.add(new Score(pid, gameCache.getGameScore(gameId, pid)));
        }
        ScoreRes scoreRes = new ScoreRes(scores);
        scoreRes.setType("scoreres");
        WebSocketUtil.broadcastToGame(gameId, scoreRes);

        // Schedule turn timer and initial state
        Map<String, Object> jobData = new HashMap<>();
        jobData.put("gameId", gameId);
        jobSchedulerService.scheduleOneTimeJob("turnTimer_" + gameId, TurnTimerJob.class,
                new Date(moveTime), jobData);

        Map<String, Object> initialMoveData = new HashMap<>();
        initialMoveData.put("gameId", gameId);
        jobSchedulerService.scheduleOneTimeJob("initialPlayerMove_" + gameId, InitialPlayerMoveJob.class,
                new Date(System.currentTimeMillis() + 2000), initialMoveData);

        log.info("Auto next round started for game {} with {} active players", gameId, players.size());
    }
}


