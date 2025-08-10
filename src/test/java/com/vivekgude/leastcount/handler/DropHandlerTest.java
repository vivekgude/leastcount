package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DropHandlerTest {

    @Mock
    private GameCache gameCache;

    @Mock
    private PlayerCache playerCache;

    private DropHandler dropHandler;

    @BeforeEach
    void setUp() {
        dropHandler = new DropHandler(gameCache, playerCache);
    }

    @Test
    void rejectsMixedRankDrop() {
        String gameId = "G1";
        long userId = 101L;

        when(gameCache.getGameState(gameId)).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getEliminatedPlayers(gameId)).thenReturn(List.of());
        when(gameCache.getCurrentPlayer(gameId)).thenReturn(userId);
        when(playerCache.getPlayerCards(gameId, String.valueOf(userId)))
                .thenReturn(List.of("7H", "8H", "9S"));

        WebSocketReq req = new WebSocketReq("dropreq",
                "{\"cards\":[\"7H\",\"8H\"]}",
                userId, "alice", gameId);

        dropHandler.handleMessage(req);

        // Ensure no mutation when invalid
        verify(playerCache, never()).setPlayerCards(anyString(), anyString(), anyList());
        verify(gameCache, never()).setOpenPile(anyString(), anyList());
    }

    @Test
    void acceptsSameRankDropAndSetsOpenPile() {
        String gameId = "G2";
        long userId = 201L;

        when(gameCache.getGameState(gameId)).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getEliminatedPlayers(gameId)).thenReturn(List.of());
        when(gameCache.getCurrentPlayer(gameId)).thenReturn(userId);
        when(playerCache.getPlayerCards(gameId, String.valueOf(userId)))
                .thenReturn(List.of("7H", "7S", "9S"));

        WebSocketReq req = new WebSocketReq("dropreq",
                "{\"cards\":[\"7H\",\"7S\"]}",
                userId, "bob", gameId);

        dropHandler.handleMessage(req);

        // hand updated and open pile replaced
        verify(playerCache, times(1)).setPlayerCards(eq(gameId), eq(String.valueOf(userId)), eq(List.of("9S")));
        verify(gameCache, times(1)).setOpenPile(eq(gameId), eq(List.of("7H", "7S")));
    }
}


