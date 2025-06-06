package com.vivekgude.leastcount.job;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.ws.response.PlayerMove;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.vivekgude.leastcount.redis.GameCache.*;

@Slf4j
@Component
public class TurnTimerJob extends BaseJob {

    private final GameCache gameCache;

    TurnTimerJob(GameCache gameCache) {
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

        // Check if move time has expired
        long moveTime = gameCache.getMoveTime(gameId);
        if (System.currentTimeMillis() >= moveTime) {
            // Get current player and players list
            long currentPlayer = gameCache.getCurrentPlayer(gameId);
            List<Long> players = gameCache.getJoinedPlayers(gameId);

            // Find next player
            int currentIndex = players.indexOf(currentPlayer);
            int nextIndex = (currentIndex + 1) % players.size();
            long nextPlayer = players.get(nextIndex);

            // Update game state
            gameCache.addFieldToMap(GAME + gameId, CURRENT_PLAYER, String.valueOf(nextPlayer));

            // Set new move time (30 seconds)
            long newMoveTime = System.currentTimeMillis() + 30000;
            gameCache.addFieldToMap(GAME + gameId, MOVE_TIME, String.valueOf(newMoveTime));

            // Notify all players
            PlayerMove playerMove = new PlayerMove(nextPlayer, newMoveTime);
            WebSocketUtil.broadcastToGame(gameId, playerMove);
        }
    }
}
