package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.dto.GameDTO;
import com.vivekgude.leastcount.model.dto.UserDataDTO;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;

import static com.vivekgude.leastcount.Constants.*;
import static com.vivekgude.leastcount.redis.GameCache.*;
import static com.vivekgude.leastcount.redis.PlayerCache.*;

public class GameServiceImpl implements GameService {

    @Autowired
    GameCache gameCache;

    @Autowired
    PlayerCache playerCache;

    @Override
    public GameDTO createGame(int userId, String userName) {
        String gameId = Utils.generateString(GAME_ID_SIZE);
        long startTime = System.currentTimeMillis() + 10 * 60 * 1000;
        gameCache.addFieldToSet(gameId, STATE, GameState.WAITING.toString());
        UserDataDTO userDataDTO = new UserDataDTO(userId, userName);
        return new GameDTO(gameId, startTime, userDataDTO, Collections.singletonList(userDataDTO));
    }

    @Override
    public GameDTO joinGame(int userId, String userName) {
        String gameId = Utils.generateString(GAME_ID_SIZE);
        long startTime = System.currentTimeMillis() + 10 * 60 * 1000;
        UserDataDTO userDataDTO = new UserDataDTO(userId, userName);
        return new GameDTO(gameId, startTime, userDataDTO, Collections.singletonList(userDataDTO));
    }

}
