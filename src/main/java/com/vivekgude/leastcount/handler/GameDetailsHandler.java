package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.dto.UserDataDTO;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.model.ws.response.GameDetailsRes;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.vivekgude.leastcount.redis.GameCache.*;
import static com.vivekgude.leastcount.redis.PlayerCache.*;

@Component
@Slf4j
public class GameDetailsHandler implements MessageHandler {

    private final GameCache gameCache;

    private final PlayerCache playerCache;

    public GameDetailsHandler(GameCache gameCache, PlayerCache playerCache) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
    }

    @Override
    public void handleMessage(WebSocketReq message) {
        try {
            String gameId = message.getGameId();
            long userId = message.getUserId();
            int gameState = gameCache.getGameState(gameId);
            List<Long> joinedPlayers = gameCache.getJoinedPlayers(gameId);
            List<UserDataDTO> playersDetails = new ArrayList<>();
            for (long playerId : joinedPlayers) {
                String playerName = playerCache.getPlayerName(gameId, playerId);
                playersDetails.add(new UserDataDTO(playerId, playerName));
            }

            long currentPlayer = 0;
            long moveTime = 0;
            if (gameState == GameState.INPROGRESS.getType()) {
                currentPlayer = gameCache.getCurrentPlayer(gameId);
                moveTime = gameCache.getMoveTime(gameId);
            }

            GameDetailsRes gameDetailsRes = new GameDetailsRes(gameState, playersDetails, currentPlayer, moveTime);

            WebSocketUtil.sendMessage(gameId, userId, gameDetailsRes);
        } catch (Exception e) {
            log.error("Error sending game details: {}", e.getMessage());
        }
    }

    @Override
    public String getMessageType() {
        return "gamedetailsreq";
    }
}
