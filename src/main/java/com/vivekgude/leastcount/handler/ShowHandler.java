package com.vivekgude.leastcount.handler;
import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.job.JobSchedulerService;
import com.vivekgude.leastcount.job.NextRoundJob;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
// use fully qualified name below to avoid import warning
import com.vivekgude.leastcount.model.ws.response.GameEnd;
import com.vivekgude.leastcount.model.ws.response.StateUpdate;
import com.vivekgude.leastcount.model.ws.response.RoundEnd;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.Utils;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.vivekgude.leastcount.constants.Constants.*;

@Slf4j
@Component
public class ShowHandler extends AbstractMessageHandler implements MessageHandler {

    private final GameCache gameCache;
    private final PlayerCache playerCache;
    private final JobSchedulerService jobSchedulerService;

    public ShowHandler(GameCache gameCache, PlayerCache playerCache, JobSchedulerService jobSchedulerService) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
        this.jobSchedulerService = jobSchedulerService;
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
            if (gameCache.getCurrentPlayer(gameId) != userId) {
                sendError(gameId, userId, "not_your_turn", "Not your turn");
                return;
            }

            // Reject mid-turn show (after a drop, before a pick)
            String lastAction = gameCache.getFieldInMap(GameCache.GAME + gameId, "lastAction");
            String lastActor = gameCache.getFieldInMap(GameCache.GAME + gameId, "lastActor");
            if ("DROP".equals(lastAction) && String.valueOf(userId).equals(lastActor)) {
                sendError(gameId, userId, "invalid_show", "Cannot declare after dropping; complete your turn first");
                return;
            }

            // Reject empty-hand show (cannot declare with zero cards)
            List<String> declarerHand = playerCache.getPlayerCards(gameId, String.valueOf(userId));
            if (declarerHand == null || declarerHand.isEmpty()) {
                sendError(gameId, userId, "invalid_show", "Cannot declare with an empty hand");
                return;
            }

            // Cancel the turn timer now — show ends the round, and we schedule NextRoundJob below.
            // Without this, TurnTimerJob may fire during WAITING and corrupt state.
            try {
                jobSchedulerService.deleteJob("turnTimer_" + gameId);
            } catch (Exception ex) {
                log.error("Failed to cancel turn timer for game {}", gameId, ex);
            }

            // Compute totals
            List<Long> players = gameCache.getActivePlayers(gameId);
            Map<Long, Integer> totals = new HashMap<>();
            for (Long pid : players) {
                List<String> hand = playerCache.getPlayerCards(gameId, String.valueOf(pid));
                totals.put(pid, Utils.computeHandTotal(hand));
            }

            int declarerTotal = totals.getOrDefault(userId, 0);
            boolean valid = true;
            for (Map.Entry<Long, Integer> e : totals.entrySet()) {
                if (e.getKey() == userId)
                    continue;
                if (declarerTotal > e.getValue()) {
                    valid = false;
                    break;
                }
            }

            // Load per-game settings with defaults
            int penalty = Optional.ofNullable(gameCache.getInvalidPenaltyOrNull(gameId))
                    .orElse(DEFAULT_INVALID_DECLARATION_PENALTY);
            int exitScore = Optional.ofNullable(gameCache.getExitScoreOrNull(gameId))
                    .orElse(DEFAULT_EXIT_SCORE);

            Map<Long, Integer> added = new HashMap<>();
            if (valid) {
                int winnerTotal = declarerTotal;
                for (Long pid : players) {
                    if (pid == userId) {
                        added.put(pid, 0);
                        continue;
                    }
                    int diff = totals.get(pid) - winnerTotal;
                    added.put(pid, Math.max(0, diff));
                }
            } else {
                int min = Integer.MAX_VALUE;
                for (int v : totals.values())
                    min = Math.min(min, v);
                int inc = penalty + Math.max(0, declarerTotal - min);
                added.put(userId, inc);
                for (Long pid : players)
                    if (pid != userId)
                        added.put(pid, 0);
            }

            // Accumulate into gameScore:{gameId} and elimination
            for (Long pid : players) {
                int current = gameCache.getGameScore(gameId, pid);
                int add = added.getOrDefault(pid, 0);
                int total = current + add;
                gameCache.setGameScore(gameId, pid, total);
                if (total >= exitScore) {
                    gameCache.addEliminated(gameId, pid);
                }
            }

            // Build typed RoundEnd payload (with new totals)
            RoundEnd round = new RoundEnd();
            round.setType("roundend");
            round.setWinnerId(valid ? userId : -1);
            round.setWinnerTotal(valid ? declarerTotal : -1);
            List<Map<String, Object>> perAdded = new ArrayList<>();
            for (Long pid : players) {
                perAdded.add(Map.of("playerId", pid, "added", added.getOrDefault(pid, 0)));
            }
            round.setPerPlayerAdded(perAdded);
            List<Map<String, Object>> totalsOut = new ArrayList<>();
            for (Long pid : players) {
                totalsOut.add(Map.of("playerId", pid, "total", gameCache.getGameScore(gameId, pid)));
            }
            round.setGameScores(totalsOut);
            WebSocketUtil.broadcastToGame(gameId, round);

            // broadcast updated state snapshot with eliminated and deck/open (typed)
            StateUpdate state = new StateUpdate();
            state.setType("stateupdate");
            state.setCurrentPlayer(gameCache.getCurrentPlayer(gameId));
            state.setMoveTime(gameCache.getMoveTime(gameId));
            state.setOpen(gameCache.getOpenPile(gameId));
            state.setDeckCount(gameCache.getDeckCount(gameId));
            state.setGameScores(gameCache.getAllGameScores(gameId));
            state.setEliminated(gameCache.getEliminatedPlayers(gameId));
            Integer roundNo = gameCache.getRoundNoOrNull(gameId);
            state.setRoundNo(roundNo == null ? 1 : roundNo);
            WebSocketUtil.broadcastToGame(gameId, state);

            // Determine next round or game end
            List<Long> actives = gameCache.getActivePlayers(gameId);
            if (actives.size() <= 1) {
                GameEnd gameend = new GameEnd();
                gameend.setType("gameend");
                gameend.setWinnerId(actives.isEmpty() ? -1 : actives.get(0));
                List<Map<String, Object>> finals = new ArrayList<>();
                for (Long pid : gameCache.getJoinedPlayers(gameId)) {
                    finals.add(Map.of("playerId", pid, "total", gameCache.getGameScore(gameId, pid)));
                }
                gameend.setFinalScores(finals);
                WebSocketUtil.broadcastToGame(gameId, gameend);
                // set game state completed
                gameCache.addFieldToMap(com.vivekgude.leastcount.redis.GameCache.GAME + gameId,
                        com.vivekgude.leastcount.redis.GameCache.STATE,
                        String.valueOf(GameState.COMPLETED.getType()));

                // cleanup keys: hands, deck, open, timers
                try {
                    // remove players' hands
                    for (Long pid : gameCache.getJoinedPlayers(gameId)) {
                        playerCache.removePlayerCards(gameId, String.valueOf(pid));
                    }
                    // clear open and deck
                    gameCache.setOpenPile(gameId, java.util.Collections.emptyList());
                    gameCache.setDeck(gameId, java.util.Collections.emptyList());
                    // delete any scheduled timers
                    jobSchedulerService.deleteJob("turnTimer_" + gameId);
                    jobSchedulerService.deleteJob("initialPlayerMove_" + gameId);
                } catch (Exception ex) {
                    log.error("Failed to cleanup game {} on end", gameId, ex);
                }
                return;
            }

            // Prepare next round: mark waiting and auto-schedule next round in 10s
            gameCache.addFieldToMap(GameCache.GAME + gameId,
                    com.vivekgude.leastcount.redis.GameCache.STATE,
                    String.valueOf(GameState.WAITING.getType()));
            // increment round number
            Integer rn = gameCache.getRoundNoOrNull(gameId);
            gameCache.setRoundNo(gameId, (rn == null ? 1 : rn + 1));

            // schedule auto next round after 10 seconds
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("gameId", gameId);
            jobSchedulerService.deleteJob("nextRound_" + gameId);
            jobSchedulerService.scheduleOneTimeJob("nextRound_" + gameId, NextRoundJob.class,
                    new java.util.Date(System.currentTimeMillis() + NEXT_ROUND_DELAY_MS), jobData);
        } catch (Exception e) {
            log.error("ShowHandler error: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getMessageType() {
        return "showreq";
    }

}


