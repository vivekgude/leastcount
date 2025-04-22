package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.model.dto.GameDTO;

public interface GameService {
    GameDTO createGame(int userId, String userName);

    GameDTO joinGame(int userId, String userName);
}
