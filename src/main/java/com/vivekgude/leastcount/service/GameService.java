package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.model.dto.GameDTO;

import java.util.Optional;

public interface GameService {
    GameDTO createGame(int userId, String userName);

    Optional<GameDTO> joinGame(int userId, String userName, String gameId);
}
