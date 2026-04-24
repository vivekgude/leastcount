package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.job.JobSchedulerService;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.service.DeckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickHandlerTest {

    @Mock
    private GameCache gameCache;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private DeckService deckService;
    @Mock
    private JobSchedulerService jobSchedulerService;

    private PickHandler pickHandler;

    @BeforeEach
    void setUp() {
        pickHandler = new PickHandler(gameCache, playerCache, deckService, jobSchedulerService);
    }

    @Test
    void pickFromOpenRemovesSpecifiedCard() {
        String gameId = "G1";
        long userId = 101L;

        when(gameCache.getGameState(gameId)).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getEliminatedPlayers(gameId)).thenReturn(List.of());
        when(gameCache.getCurrentPlayer(gameId)).thenReturn(userId);
        when(gameCache.getFieldInMap("game:" + gameId, "lastAction")).thenReturn("DROP");
        when(gameCache.getFieldInMap("game:" + gameId, "lastActor")).thenReturn(String.valueOf(userId));
        when(gameCache.removeFromOpenPile(gameId, "7H")).thenReturn(true);
        when(playerCache.getPlayerCards(gameId, String.valueOf(userId))).thenReturn(List.of("9S"));
        when(gameCache.getActivePlayers(gameId)).thenReturn(List.of(userId, 102L));

        WebSocketReq req = new WebSocketReq("pickreq",
                "{\"source\":\"open\",\"card\":\"7H\"}",
                userId, "alice", gameId);

        pickHandler.handleMessage(req);

        verify(gameCache, times(1)).removeFromOpenPile(gameId, "7H");
        verify(playerCache, times(1)).setPlayerCards(eq(gameId), eq(String.valueOf(userId)), eq(List.of("9S", "7H")));
        verify(gameCache, times(1)).addFieldToMap(eq("game:" + gameId), eq("currentPlayer"), eq("102"));
    }

    @Test
    void pickFromClosedUsesDeckServiceAndRebuildsWhenEmpty() {
        String gameId = "G2";
        long userId = 201L;

        when(gameCache.getGameState(gameId)).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getEliminatedPlayers(gameId)).thenReturn(List.of());
        when(gameCache.getCurrentPlayer(gameId)).thenReturn(userId);
        when(gameCache.getFieldInMap("game:" + gameId, "lastAction")).thenReturn("DROP");
        when(gameCache.getFieldInMap("game:" + gameId, "lastActor")).thenReturn(String.valueOf(userId));
        when(deckService.drawFromClosedPile(gameId)).thenReturn("5D");
        when(playerCache.getPlayerCards(gameId, String.valueOf(userId))).thenReturn(List.of());
        when(gameCache.getActivePlayers(gameId)).thenReturn(List.of(userId, 202L));

        WebSocketReq req = new WebSocketReq("pickreq",
                "{\"source\":\"closed\"}",
                userId, "bob", gameId);

        pickHandler.handleMessage(req);

        verify(deckService, times(1)).drawFromClosedPile(gameId);
        verify(playerCache, times(1)).setPlayerCards(eq(gameId), eq(String.valueOf(userId)), eq(List.of("5D")));
        verify(gameCache, times(1)).addFieldToMap(eq("game:" + gameId), eq("currentPlayer"), eq("202"));
    }
}


