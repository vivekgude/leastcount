package com.vivekgude.leastcount.integration;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.handler.DropHandler;
import com.vivekgude.leastcount.handler.PickHandler;
import com.vivekgude.leastcount.handler.ShowHandler;
import com.vivekgude.leastcount.job.JobSchedulerService;
import com.vivekgude.leastcount.job.TurnTimerJob;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.service.DeckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.vivekgude.leastcount.redis.GameCache.GAME;
import static com.vivekgude.leastcount.redis.GameCache.MOVE_TIME;
import static com.vivekgude.leastcount.redis.GameCache.CURRENT_PLAYER;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationFlowTest {

    @Mock private GameCache gameCache;
    @Mock private PlayerCache playerCache;
    @Mock private DeckService deckService;
    @Mock private JobSchedulerService jobSchedulerService;

    private DropHandler dropHandler;
    private PickHandler pickHandler;
    private ShowHandler showHandler;

    @BeforeEach
    void setUp() {
        dropHandler = new DropHandler(gameCache, playerCache);
        pickHandler = new PickHandler(gameCache, playerCache, deckService, jobSchedulerService);
        showHandler = new ShowHandler(gameCache, playerCache, jobSchedulerService);
    }

    @Test
    void fullTurnCycle_dropThenPickFromOpen_thenRotateTurn() throws Exception {
        String gameId = "G1";
        long p1 = 1L, p2 = 2L;

        // Common state
        when(gameCache.getGameState(gameId)).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getEliminatedPlayers(gameId)).thenReturn(List.of());
        when(gameCache.getCurrentPlayer(gameId)).thenReturn(p1);

        // P1 hand before drop
        when(playerCache.getPlayerCards(gameId, String.valueOf(p1)))
                .thenReturn(new ArrayList<>(List.of("7H", "9S")));

        // Execute drop of 7H
        WebSocketReq dropReq = new WebSocketReq("dropreq", "{\"cards\":[\"7H\"]}", p1, "p1", gameId);
        dropHandler.handleMessage(dropReq);

        // Verify hand updated and open set
        verify(playerCache).setPlayerCards(eq(gameId), eq(String.valueOf(p1)), eq(List.of("9S")));
        verify(gameCache).setOpenPile(eq(gameId), eq(List.of("7H")));

        // Prepare pick from open: DropHandler marks lastAction/lastActor in cache, emulate reads
        when(gameCache.getFieldInMap(GAME + gameId, "lastAction")).thenReturn("DROP");
        when(gameCache.getFieldInMap(GAME + gameId, "lastActor")).thenReturn(String.valueOf(p1));

        // remove card from open succeeds
        when(gameCache.removeFromOpenPile(gameId, "7H")).thenReturn(true);
        // hand before pick
        when(playerCache.getPlayerCards(gameId, String.valueOf(p1)))
                .thenReturn(new ArrayList<>(List.of("9S")));
        // players for rotation
        when(gameCache.getActivePlayers(gameId)).thenReturn(List.of(p1, p2));

        WebSocketReq pickReq = new WebSocketReq("pickreq", "{\"source\":\"open\",\"card\":\"7H\"}", p1, "p1", gameId);
        pickHandler.handleMessage(pickReq);

        // Verify card added and turn advanced to p2
        verify(playerCache).setPlayerCards(eq(gameId), eq(String.valueOf(p1)), eq(List.of("9S", "7H")));
        verify(gameCache).addFieldToMap(eq(GAME + gameId), eq(CURRENT_PLAYER), eq(String.valueOf(p2)));
        verify(gameCache).addFieldToMap(eq(GAME + gameId), eq(MOVE_TIME), anyString());
        verify(jobSchedulerService).deleteJob("turnTimer_" + gameId);
        verify(jobSchedulerService).scheduleOneTimeJob(eq("turnTimer_" + gameId), eq(TurnTimerJob.class), any(), anyMap());
    }

    @Test
    void show_validDeclaration_schedulesNextRound_whenMultipleActives() throws Exception {
        String gameId = "G2";
        long p1 = 1L, p2 = 2L, p3 = 3L;

        when(gameCache.getGameState(gameId)).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getCurrentPlayer(gameId)).thenReturn(p1);
        when(gameCache.getActivePlayers(gameId)).thenReturn(List.of(p1, p2, p3));

        // Totals: p1=2, p2=5, p3=9 so valid declaration
        when(playerCache.getPlayerCards(gameId, String.valueOf(p1))).thenReturn(List.of("2H"));
        when(playerCache.getPlayerCards(gameId, String.valueOf(p2))).thenReturn(List.of("5S"));
        when(playerCache.getPlayerCards(gameId, String.valueOf(p3))).thenReturn(List.of("9D"));

        // Scores start at 0
        when(gameCache.getGameScore(gameId, p1)).thenReturn(0);
        when(gameCache.getGameScore(gameId, p2)).thenReturn(0);
        when(gameCache.getGameScore(gameId, p3)).thenReturn(0);

        // Exit score high to avoid game end
        when(gameCache.getExitScoreOrNull(gameId)).thenReturn(100);
        when(gameCache.getInvalidPenaltyOrNull(gameId)).thenReturn(40);
        when(gameCache.getRoundNoOrNull(gameId)).thenReturn(1);

        WebSocketReq showReq = new WebSocketReq("showreq", "{}", p1, "p1", gameId);
        showHandler.handleMessage(showReq);

        // Ensure scores updated: p2 += 3, p3 += 7
        verify(gameCache).setGameScore(gameId, p1, 0);
        verify(gameCache).setGameScore(gameId, p2, 3);
        verify(gameCache).setGameScore(gameId, p3, 7);

        // State set to WAITING and next round scheduled
        verify(gameCache).addFieldToMap(eq(GAME + gameId), eq("state"), eq(String.valueOf(GameState.WAITING.getType())));
        verify(jobSchedulerService).deleteJob("nextRound_" + gameId);
        verify(jobSchedulerService).scheduleOneTimeJob(eq("nextRound_" + gameId), any(), any(), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    void show_gameEnds_cleansUpAndCompletes() throws Exception {
        String gameId = "G3";
        long p1 = 10L, p2 = 20L;

        when(gameCache.getGameState(gameId)).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getCurrentPlayer(gameId)).thenReturn(p1);
        // First call for totals uses both players, second call after scoring returns only winner active
        when(gameCache.getActivePlayers(gameId)).thenReturn(List.of(p1, p2), List.of(p1));

        // p1 valid declaration over p2
        when(playerCache.getPlayerCards(gameId, String.valueOf(p1))).thenReturn(List.of("2H"));
        when(playerCache.getPlayerCards(gameId, String.valueOf(p2))).thenReturn(List.of("9S"));

        // Ensure p2 crosses exit score on add
        when(gameCache.getExitScoreOrNull(gameId)).thenReturn(5);
        when(gameCache.getInvalidPenaltyOrNull(gameId)).thenReturn(40);
        when(gameCache.getGameScore(gameId, p1)).thenReturn(0);
        when(gameCache.getGameScore(gameId, p2)).thenReturn(3);
        when(gameCache.getJoinedPlayers(gameId)).thenReturn(List.of(p1, p2));

        WebSocketReq showReq = new WebSocketReq("showreq", "{}", p1, "p1", gameId);
        showHandler.handleMessage(showReq);

        // Winner decided, game completed
        verify(gameCache).addFieldToMap(eq(GAME + gameId), eq("state"), eq(String.valueOf(GameState.COMPLETED.getType())));

        // Cleanup
        verify(playerCache).removePlayerCards(gameId, String.valueOf(p1));
        verify(playerCache).removePlayerCards(gameId, String.valueOf(p2));
        verify(gameCache).setOpenPile(gameId, Collections.emptyList());
        verify(gameCache).setDeck(gameId, Collections.emptyList());
        // turnTimer is deleted twice: once at show start (B2 fix), once in game-end cleanup
        verify(jobSchedulerService, times(2)).deleteJob("turnTimer_" + gameId);
        verify(jobSchedulerService).deleteJob("initialPlayerMove_" + gameId);
    }
}


