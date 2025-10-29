package com.vivekgude.leastcount.handler;

import com.google.gson.Gson;
import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.WebSocketUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DropHandlerTest {

    @Mock
    private GameCache gameCache;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private WebSocketUtil webSocketUtil;

    private DropHandler dropHandler;
    private Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dropHandler = new DropHandler(gameCache, playerCache);
    }

    @Test
    @DisplayName("Should reject drop when game is not in progress")
    void testDropWhenGameNotInProgress() {
        // Arrange
        WebSocketReq message = createMessage("test-game", 123L, "{\"cards\":[\"1H\",\"1S\"]}");
        when(gameCache.getGameState("test-game")).thenReturn(GameState.WAITING.getType());

        // Act
        dropHandler.handleMessage(message);

        // Assert
        verify(gameCache, never()).setOpenPile(anyString(), anyList());
        verify(playerCache, never()).setPlayerCards(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Should reject drop when not player's turn")
    void testDropWhenNotPlayerTurn() {
        // Arrange
        WebSocketReq message = createMessage("test-game", 123L, "{\"cards\":[\"1H\",\"1S\"]}");
        when(gameCache.getGameState("test-game")).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getEliminatedPlayers("test-game")).thenReturn(Collections.emptyList());
        when(gameCache.getCurrentPlayer("test-game")).thenReturn(456L); // Different player

        // Act
        dropHandler.handleMessage(message);

        // Assert
        verify(gameCache, never()).setOpenPile(anyString(), anyList());
        verify(playerCache, never()).setPlayerCards(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Should reject drop when player is eliminated")
    void testDropWhenPlayerEliminated() {
        // Arrange
        WebSocketReq message = createMessage("test-game", 123L, "{\"cards\":[\"1H\",\"1S\"]}");
        when(gameCache.getGameState("test-game")).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getEliminatedPlayers("test-game")).thenReturn(Arrays.asList(123L));
        when(gameCache.getCurrentPlayer("test-game")).thenReturn(123L);

        // Act
        dropHandler.handleMessage(message);

        // Assert
        verify(gameCache, never()).setOpenPile(anyString(), anyList());
        verify(playerCache, never()).setPlayerCards(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Should reject drop when player doesn't own all cards")
    void testDropWhenPlayerDoesntOwnCards() {
        // Arrange
        WebSocketReq message = createMessage("test-game", 123L, "{\"cards\":[\"1H\",\"1S\"]}");
        when(gameCache.getGameState("test-game")).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getEliminatedPlayers("test-game")).thenReturn(Collections.emptyList());
        when(gameCache.getCurrentPlayer("test-game")).thenReturn(123L);
        when(playerCache.getPlayerCards("test-game", "123")).thenReturn(Arrays.asList("1H", "2S")); // Different cards

        // Act
        dropHandler.handleMessage(message);

        // Assert
        verify(gameCache, never()).setOpenPile(anyString(), anyList());
        verify(playerCache, never()).setPlayerCards(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Should reject drop when cards are not same rank")
    void testDropWhenCardsNotSameRank() {
        // Arrange
        WebSocketReq message = createMessage("test-game", 123L, "{\"cards\":[\"1H\",\"2S\"]}");
        when(gameCache.getGameState("test-game")).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getEliminatedPlayers("test-game")).thenReturn(Collections.emptyList());
        when(gameCache.getCurrentPlayer("test-game")).thenReturn(123L);
        when(playerCache.getPlayerCards("test-game", "123")).thenReturn(Arrays.asList("1H", "2S"));

        // Act
        dropHandler.handleMessage(message);

        // Assert
        verify(gameCache, never()).setOpenPile(anyString(), anyList());
        verify(playerCache, never()).setPlayerCards(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Should accept valid drop with same rank cards")
    void testValidDrop() {
        // Arrange
        WebSocketReq message = createMessage("test-game", 123L, "{\"cards\":[\"1H\",\"1S\"]}");
        when(gameCache.getGameState("test-game")).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getEliminatedPlayers("test-game")).thenReturn(Collections.emptyList());
        when(gameCache.getCurrentPlayer("test-game")).thenReturn(123L);
        when(playerCache.getPlayerCards("test-game", "123")).thenReturn(Arrays.asList("1H", "1S", "2D"));
        when(gameCache.getDeckCount("test-game")).thenReturn(50);

        // Act
        dropHandler.handleMessage(message);

        // Assert
        verify(playerCache).setPlayerCards("test-game", "123", Arrays.asList("2D"));
        verify(gameCache).setOpenPile("test-game", Arrays.asList("1H", "1S"));
    }

    @Test
    @DisplayName("Should reject drop with empty cards")
    void testDropWithEmptyCards() {
        // Arrange
        WebSocketReq message = createMessage("test-game", 123L, "{\"cards\":[]}");
        when(gameCache.getGameState("test-game")).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getEliminatedPlayers("test-game")).thenReturn(Collections.emptyList());
        when(gameCache.getCurrentPlayer("test-game")).thenReturn(123L);

        // Act
        dropHandler.handleMessage(message);

        // Assert
        verify(gameCache, never()).setOpenPile(anyString(), anyList());
        verify(playerCache, never()).setPlayerCards(anyString(), anyString(), anyList());
    }

    private WebSocketReq createMessage(String gameId, Long userId, String content) {
        WebSocketReq message = new WebSocketReq();
        message.setGameId(gameId);
        message.setUserId(userId);
        message.setContent(content);
        return message;
    }
}