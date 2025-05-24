package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.model.dto.GameDTO;

import java.util.Optional;

public interface GameService {
    GameDTO createGame(long userId, String userName);

    Optional<GameDTO> joinGame(long userId, String userName, String gameId);
}
