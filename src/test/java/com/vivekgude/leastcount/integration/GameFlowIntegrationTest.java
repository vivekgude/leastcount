package com.vivekgude.leastcount.integration;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.dto.GameConfig;
import com.vivekgude.leastcount.model.dto.GameDTO;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.service.DeckService;
import com.vivekgude.leastcount.service.GameService;
import com.vivekgude.leastcount.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class GameFlowIntegrationTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameCache gameCache;

    @Autowired
    private PlayerCache playerCache;

    @Autowired
    private DeckService deckService;

    private String gameId;
    private Long player1Id = 123L;
    private Long player2Id = 456L;
    private String player1Name = "Player1";
    private String player2Name = "Player2";

    @BeforeEach
    void setUp() {
        // Create a game with custom configuration
        GameConfig config = new GameConfig();
        config.setCardsPerPlayer(3);
        config.setExitScore(50);
        config.setInvalidDeclarationPenalty(20);
        config.setMoveTimeMs(10000L);

        GameDTO game = gameService.createGame(player1Id, player1Name, config);
        gameId = game.getGameId();

        // Join second player
        gameService.joinGame(player2Id, player2Name, gameId);
    }

    @Test
    @DisplayName("Should create game with custom configuration")
    void testGameCreationWithCustomConfig() {
        // Verify game configuration
        assertEquals(3, gameCache.getCardsPerPlayerOrNull(gameId));
        assertEquals(50, gameCache.getExitScoreOrNull(gameId));
        assertEquals(20, gameCache.getInvalidPenaltyOrNull(gameId));
        assertEquals(10000L, gameCache.getMoveTimeConfigOrNull(gameId));
        assertEquals(10, gameCache.getGameState(gameId)); // WAITING
    }

    @Test
    @DisplayName("Should start game and deal cards correctly")
    void testGameStartAndCardDealing() {
        // Start the game (this would normally be done via WebSocket)
        // For testing, we'll simulate the start game logic
        List<Long> players = gameCache.getJoinedPlayers(gameId);
        assertEquals(2, players.size());

        // Simulate game start
        gameCache.addFieldToMap("game:" + gameId, "state", String.valueOf(GameState.INPROGRESS.getType()));
        gameCache.addFieldToMap("game:" + gameId, "currentPlayer", String.valueOf(player1Id));
        gameCache.addFieldToMap("game:" + gameId, "roundNo", "1");

        // Generate and set deck
        List<String> deck = Utils.generateTwoDecksShuffled();
        gameCache.setDeck(gameId, deck);

        // Deal cards to players
        int cardsPerPlayer = gameCache.getCardsPerPlayerOrNull(gameId);
        for (Long playerId : players) {
            List<String> playerCards = deck.subList(0, cardsPerPlayer);
            deck = deck.subList(cardsPerPlayer, deck.size());
            playerCache.setPlayerCards(gameId, String.valueOf(playerId), playerCards);
        }

        // Set remaining deck
        gameCache.setDeck(gameId, deck);

        // Verify game state
        assertEquals(GameState.INPROGRESS.getType(), gameCache.getGameState(gameId));
        assertEquals(player1Id, gameCache.getCurrentPlayer(gameId));
        assertEquals(1, gameCache.getRoundNoOrNull(gameId));

        // Verify cards were dealt
        List<String> player1Cards = playerCache.getPlayerCards(gameId, String.valueOf(player1Id));
        List<String> player2Cards = playerCache.getPlayerCards(gameId, String.valueOf(player2Id));
        assertEquals(3, player1Cards.size());
        assertEquals(3, player2Cards.size());

        // Verify deck count
        assertEquals(98, gameCache.getDeckCount(gameId)); // 104 - 6 dealt cards
    }

    @Test
    @DisplayName("Should handle deck rebuilding when closed pile is empty")
    void testDeckRebuilding() {
        // Set up a game with empty closed pile
        gameCache.setDeck(gameId, List.of());
        gameCache.setOpenPile(gameId, List.of("1H", "1S"));

        // Set up player hands
        playerCache.setPlayerCards(gameId, String.valueOf(player1Id), List.of("2H", "2S"));
        playerCache.setPlayerCards(gameId, String.valueOf(player2Id), List.of("3H", "3S"));

        // Try to draw from closed pile
        String card = deckService.drawFromClosedPile(gameId);

        // Should rebuild deck and return a card
        assertNotNull(card);
        assertTrue(gameCache.getDeckCount(gameId) > 0);
    }

    @Test
    @DisplayName("Should calculate hand totals correctly")
    void testHandTotalCalculation() {
        List<String> cards = List.of("1H", "10S", "11D", "12C", "13H");
        int total = Utils.computeHandTotal(cards);
        assertEquals(47, total); // 1 + 10 + 11 + 12 + 13
    }

    @Test
    @DisplayName("Should validate same rank cards correctly")
    void testSameRankValidation() {
        assertTrue(Utils.allSameRank(List.of("1H", "1S", "1D", "1C")));
        assertTrue(Utils.allSameRank(List.of("10H", "10S")));
        assertFalse(Utils.allSameRank(List.of("1H", "2S")));
        assertFalse(Utils.allSameRank(List.of("10H", "11S")));
    }
}
