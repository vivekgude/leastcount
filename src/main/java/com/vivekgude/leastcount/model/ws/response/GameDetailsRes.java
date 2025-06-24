package com.vivekgude.leastcount.model.ws.response;

import com.vivekgude.leastcount.model.dto.UserDataDTO;
import com.vivekgude.leastcount.model.ws.WebSocketRes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameDetailsRes extends WebSocketRes {
    private int gameState;
    private UserDataDTO host;
    private List<UserDataDTO> players;
    private long currentPlayer;
    private long moveTime;
}
