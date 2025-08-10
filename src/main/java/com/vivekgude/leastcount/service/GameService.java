package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.model.dto.GameDTO;
import com.vivekgude.leastcount.model.dto.GameConfig;

import java.util.Optional;

public interface GameService {
    GameDTO createGame(long userId, String userName, GameConfig overrides);

    Optional<GameDTO> joinGame(long userId, String userName, String gameId);

    boolean exitGame(long userId, String userName, String gameId);
}
