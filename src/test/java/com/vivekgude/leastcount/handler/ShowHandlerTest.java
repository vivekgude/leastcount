package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.job.JobSchedulerService;
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
class ShowHandlerTest {

    @Mock
    private GameCache gameCache;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private JobSchedulerService jobSchedulerService;

    private ShowHandler showHandler;

    @BeforeEach
    void setUp() {
        showHandler = new ShowHandler(gameCache, playerCache, jobSchedulerService);
    }

    @Test
    void validDeclarationAddsZeroToWinnerAndPositivesToOthers() {
        String gameId = "G1";
        long declarer = 101L;
        long other = 102L;

        when(gameCache.getGameState(gameId)).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getCurrentPlayer(gameId)).thenReturn(declarer);
        when(gameCache.getActivePlayers(gameId)).thenReturn(List.of(declarer, other));
        when(playerCache.getPlayerCards(gameId, String.valueOf(declarer))).thenReturn(List.of("2H"));
        when(playerCache.getPlayerCards(gameId, String.valueOf(other))).thenReturn(List.of("5S"));
        when(gameCache.getInvalidPenaltyOrNull(gameId)).thenReturn(40);
        when(gameCache.getExitScoreOrNull(gameId)).thenReturn(100);
        when(gameCache.getGameScore(gameId, declarer)).thenReturn(0);
        when(gameCache.getGameScore(gameId, other)).thenReturn(0);

        WebSocketReq req = new WebSocketReq("showreq", "{}", declarer, "alice", gameId);
        showHandler.handleMessage(req);

        // winner adds 0, other adds positive (5-2)=3
        verify(gameCache, times(1)).setGameScore(gameId, declarer, 0);
        verify(gameCache, times(1)).setGameScore(gameId, other, 3);
        verify(gameCache, never()).addEliminated(eq(gameId), anyLong());
    }

    @Test
    void invalidDeclarationAddsPenaltyAndCanEliminate() {
        String gameId = "G2";
        long declarer = 201L;
        long other = 202L;

        when(gameCache.getGameState(gameId)).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getCurrentPlayer(gameId)).thenReturn(declarer);
        when(gameCache.getActivePlayers(gameId)).thenReturn(List.of(declarer, other));
        // declarer higher total than other
        when(playerCache.getPlayerCards(gameId, String.valueOf(declarer))).thenReturn(List.of("9H"));
        when(playerCache.getPlayerCards(gameId, String.valueOf(other))).thenReturn(List.of("2S"));
        when(gameCache.getInvalidPenaltyOrNull(gameId)).thenReturn(40);
        when(gameCache.getExitScoreOrNull(gameId)).thenReturn(45);
        when(gameCache.getGameScore(gameId, declarer)).thenReturn(10);
        when(gameCache.getGameScore(gameId, other)).thenReturn(0);

        WebSocketReq req = new WebSocketReq("showreq", "{}", declarer, "bob", gameId);
        showHandler.handleMessage(req);

        // penalty 40 + (9-2)=7 => 47 added to declarer, total becomes 57 => eliminated
        verify(gameCache, times(1)).setGameScore(gameId, declarer, 57);
        verify(gameCache, times(1)).addEliminated(gameId, declarer);
        verify(gameCache, times(1)).setGameScore(gameId, other, 0);
    }
}


