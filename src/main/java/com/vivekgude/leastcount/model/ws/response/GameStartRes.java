package com.vivekgude.leastcount.model.ws.response;

import com.vivekgude.leastcount.model.ws.WebSocketRes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStartRes extends WebSocketRes {
    private int gameState;
    private long currentPlayer;
    private long moveTime;
} 