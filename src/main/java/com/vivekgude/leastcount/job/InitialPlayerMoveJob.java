package com.vivekgude.leastcount.job;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import com.vivekgude.leastcount.model.ws.response.StateUpdate;

@Slf4j
@Component
public class InitialPlayerMoveJob extends BaseJob {

    private final GameCache gameCache;

    InitialPlayerMoveJob(GameCache gameCache) {
        this.gameCache = gameCache;
    }

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        String gameId = context.getJobDetail().getJobDataMap().getString("gameId");

        // Check if game is still in progress
        int gameState = gameCache.getGameState(gameId);
        if (gameState != GameState.INPROGRESS.getType()) {
            return;
        }

        // Get current player and move time
        long currentPlayer = gameCache.getCurrentPlayer(gameId);
        long moveTime = gameCache.getMoveTime(gameId);

        // Send initial stateupdate snapshot (typed)
        StateUpdate state = new StateUpdate();
        state.setType("stateupdate");
        state.setCurrentPlayer(currentPlayer);
        state.setMoveTime(moveTime);
        WebSocketUtil.broadcastToGame(gameId, state);
    }
} 