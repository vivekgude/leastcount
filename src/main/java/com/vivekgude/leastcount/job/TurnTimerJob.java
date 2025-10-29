package com.vivekgude.leastcount.job;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.ws.response.StateUpdate;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

import static com.vivekgude.leastcount.redis.GameCache.*;

@Slf4j
@Component
public class TurnTimerJob extends BaseJob {

    private final GameCache gameCache;
    private final JobSchedulerService jobSchedulerService;

    TurnTimerJob(GameCache gameCache, JobSchedulerService jobSchedulerService) {
        this.gameCache = gameCache;
        this.jobSchedulerService = jobSchedulerService;
    }

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        String gameId = context.getJobDetail().getJobDataMap().getString("gameId");

        // Check if game is still in progress
        int gameState = gameCache.getGameState(gameId);
        if (gameState != GameState.INPROGRESS.getType()) {
            return;
        }

        // Check if move time has expired
        long moveTime = gameCache.getMoveTime(gameId);
        if (System.currentTimeMillis() >= moveTime) {
            // Get current player and players list
            long currentPlayer = gameCache.getCurrentPlayer(gameId);
            List<Long> players = gameCache.getActivePlayers(gameId);

            // Find next player
            int currentIndex = players.indexOf(currentPlayer);
            int nextIndex = (currentIndex + 1) % players.size();
            long nextPlayer = players.get(nextIndex);

            // Update game state
            gameCache.addFieldToMap(GAME + gameId, CURRENT_PLAYER, String.valueOf(nextPlayer));

            // Set new move time from config or default
            Long configured = gameCache.getMoveTimeConfigOrNull(gameId);
            long newMoveTime = System.currentTimeMillis() + (configured != null ? configured : 30000);
            gameCache.addFieldToMap(GAME + gameId, MOVE_TIME, String.valueOf(newMoveTime));

            // Clear turn markers so next player can start fresh (timer expired, so previous player's turn is over)
            gameCache.addFieldToMap(GAME + gameId, "lastAction", "NONE");
            gameCache.addFieldToMap(GAME + gameId, "lastActor", "0");

            // Notify all players with stateupdate
            StateUpdate state = new StateUpdate();
            state.setType("stateupdate");
            state.setCurrentPlayer(nextPlayer);
            state.setMoveTime(newMoveTime);
            WebSocketUtil.broadcastToGame(gameId, state);

            // Reschedule next timer
            try {
                Map<String, Object> jobData = new HashMap<>();
                jobData.put("gameId", gameId);
                jobSchedulerService.deleteJob("turnTimer_" + gameId);
                jobSchedulerService.scheduleOneTimeJob("turnTimer_" + gameId, TurnTimerJob.class,
                        new Date(newMoveTime), jobData);
            } catch (Exception ex) {
                log.error("Failed to reschedule next timer for game {}", gameId, ex);
            }
        }
    }

}
