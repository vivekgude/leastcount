package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.dto.GameDTO;
import com.vivekgude.leastcount.model.dto.UserDataDTO;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.vivekgude.leastcount.constants.Constants.*;
import static com.vivekgude.leastcount.redis.GameCache.*;
import static com.vivekgude.leastcount.redis.PlayerCache.*;

@Service
public class GameServiceImpl implements GameService {

    private final GameCache gameCache;

    private final PlayerCache playerCache;

    public GameServiceImpl(GameCache gameCache, PlayerCache playerCache) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
    }

    @Override
    public GameDTO createGame(long userId, String userName) {
        String gameId = Utils.generateString(GAME_ID_SIZE);
        //        long startTime = System.currentTimeMillis() + 10 * 60 * 1000;
        gameCache.addFieldToMap(GAME + gameId, STATE, GameState.WAITING.toString());
        gameCache.addFieldToMap(GAME + gameId, HOST, String.valueOf(userId));
        gameCache.addFieldToMap(GAME + gameId, HOST_NAME, userName);
        gameCache.addValuesInSet(JOINED_PLAYERS + gameId, String.valueOf(userId));
        playerCache.addFieldToMap(PLAYER + userId + ":" + GAME + gameId, PLAYER_NAME, userName);
        UserDataDTO userDataDTO = new UserDataDTO(userId, userName);
        return new GameDTO(gameId, userDataDTO, Collections.singletonList(userDataDTO));
    }

    @Override
    public Optional<GameDTO> joinGame(long userId, String userName, String gameId) {
        int gameState = gameCache.getGameState(gameId);
        if (gameState == GameState.INVALID.getType()) {
            return Optional.empty();
        } else if (gameState == GameState.WAITING.getType()) {
            List<Long> joinedPlayers = gameCache.getJoinedPlayers(gameId);
            if (!joinedPlayers.contains(userId)) {
                gameCache.addValuesInSet(JOINED_PLAYERS + gameId, String.valueOf(userId));
                playerCache.addFieldToMap(PLAYER + userId + ":" + GAME + gameId, PLAYER_NAME, userName);
                long host = Long.parseLong(gameCache.getFieldInMap(GAME + gameId, HOST));
                String hostName = gameCache.getFieldInMap(GAME + gameId, HOST_NAME);
//                List<Long> joinedPlayers = gameCache.getJoinedPlayers(gameId);
                UserDataDTO userDataDTO = new UserDataDTO(host, hostName);
                return Optional.of(new GameDTO(gameId, userDataDTO, Collections.singletonList(userDataDTO)));
            }
            return Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean exitGame(long userId, String userName, String gameId) {
        int gameState = gameCache.getGameState(gameId);
        if (gameState == GameState.WAITING.getType()) {
            List<Long> joinedPlayers = gameCache.getJoinedPlayers(gameId);
            if (joinedPlayers.contains(userId)) {
                gameCache.removeValuesInSet(JOINED_PLAYERS + gameId, String.valueOf(userId));
                playerCache.deleteKeys(PLAYER + userId + ":" + GAME + gameId);
                return true;
            }
        }
        return false;
    }

}
