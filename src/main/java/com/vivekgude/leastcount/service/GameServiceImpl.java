package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.dto.GameDTO;
import com.vivekgude.leastcount.model.dto.UserDataDTO;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.vivekgude.leastcount.constants.Constants.*;
import static com.vivekgude.leastcount.redis.GameCache.*;

public class GameServiceImpl implements GameService {

    @Autowired
    GameCache gameCache;

    @Autowired
    PlayerCache playerCache;

    @Override
    public GameDTO createGame(int userId, String userName) {
        String gameId = Utils.generateString(GAME_ID_SIZE);
        //        long startTime = System.currentTimeMillis() + 10 * 60 * 1000;
        gameCache.addFieldToMap(GAME + gameId, STATE, GameState.WAITING.toString());
        gameCache.addFieldToMap(GAME + gameId, HOST, String.valueOf(userId));
        gameCache.addFieldToMap(GAME + gameId, HOST_NAME, userName);
        gameCache.addValuesInSet(JOINED_PLAYERS + gameId, String.valueOf(userId));
        UserDataDTO userDataDTO = new UserDataDTO(userId, userName);
        return new GameDTO(gameId, userDataDTO, Collections.singletonList(userDataDTO));
    }

    @Override
    public Optional<GameDTO> joinGame(int userId, String userName, String gameId) {
        int gameState = gameCache.getGameState(gameId);
        if (gameState == GameState.INVALID.getType()) {
            return Optional.empty();
        } else if (gameState == GameState.STARTING.getType()) {
            gameCache.addValuesInSet(JOINED_PLAYERS + gameId, String.valueOf(userId));
            long host = Long.parseLong(gameCache.getFieldInMap(GAME + gameId, HOST));
            String hostName = gameCache.getFieldInMap(GAME + gameId, HOST_NAME);
            List<Long> joinedPlayers = gameCache.getJoinedPlayers(gameId);
            UserDataDTO userDataDTO = new UserDataDTO(host, hostName);
            return Optional.of(new GameDTO(gameId, userDataDTO, Collections.singletonList(userDataDTO)));
        } else {
            return Optional.empty();
        }
    }

}
