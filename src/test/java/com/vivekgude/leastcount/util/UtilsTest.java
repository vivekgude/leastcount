package com.vivekgude.leastcount.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

class UtilsTest {

    @Test
    @DisplayName("Should generate shuffled decks with correct size")
    void testGenerateShuffledDecks() {
        List<String> deck = Utils.generateShuffledDecks(1);
        assertEquals(52, deck.size());
        
        List<String> twoDecks = Utils.generateShuffledDecks(2);
        assertEquals(104, twoDecks.size());
    }

    @Test
    @DisplayName("Should generate two decks shuffled")
    void testGenerateTwoDecksShuffled() {
        List<String> deck = Utils.generateTwoDecksShuffled();
        assertEquals(104, deck.size());
    }

    @Test
    @DisplayName("Should compute hand total correctly for all card values")
    void testComputeHandTotal() {
        // Test Ace (1)
        assertEquals(1, Utils.computeHandTotal(Arrays.asList("1H")));
        
        // Test number cards (2-10)
        assertEquals(2, Utils.computeHandTotal(Arrays.asList("2H")));
        assertEquals(10, Utils.computeHandTotal(Arrays.asList("10H")));
        
        // Test face cards
        assertEquals(11, Utils.computeHandTotal(Arrays.asList("11H"))); // Jack
        assertEquals(12, Utils.computeHandTotal(Arrays.asList("12H"))); // Queen
        assertEquals(13, Utils.computeHandTotal(Arrays.asList("13H"))); // King
        
        // Test multiple cards
        assertEquals(6, Utils.computeHandTotal(Arrays.asList("1H", "2S", "3D")));
        assertEquals(36, Utils.computeHandTotal(Arrays.asList("11H", "12S", "13D")));
        
        // Test empty/null
        assertEquals(0, Utils.computeHandTotal(Arrays.asList()));
        assertEquals(0, Utils.computeHandTotal(null));
    }

    @Test
    @DisplayName("Should check if all cards have same rank")
    void testAllSameRank() {
        // Same rank
        assertTrue(Utils.allSameRank(Arrays.asList("1H", "1S", "1D", "1C")));
        assertTrue(Utils.allSameRank(Arrays.asList("10H", "10S")));
        assertTrue(Utils.allSameRank(Arrays.asList("11H", "11S", "11D")));
        
        // Different ranks
        assertFalse(Utils.allSameRank(Arrays.asList("1H", "2S")));
        assertFalse(Utils.allSameRank(Arrays.asList("10H", "11S", "12D")));
        assertFalse(Utils.allSameRank(Arrays.asList("1H", "1S", "2D")));
        
        // Edge cases
        assertFalse(Utils.allSameRank(Arrays.asList()));
        assertFalse(Utils.allSameRank(null));
        assertTrue(Utils.allSameRank(Arrays.asList("1H"))); // Single card
    }

    @Test
    @DisplayName("Should extract rank from card codes")
    void testExtractRank() {
        assertEquals("1", Utils.extractRank("1H"));
        assertEquals("10", Utils.extractRank("10S"));
        assertEquals("11", Utils.extractRank("11D"));
        assertEquals("12", Utils.extractRank("12C"));
        assertEquals("13", Utils.extractRank("13H"));
        assertEquals("5", Utils.extractRank("5S"));
        
        // Edge cases
        assertEquals("", Utils.extractRank(""));
        assertEquals("", Utils.extractRank(null));
    }
}
