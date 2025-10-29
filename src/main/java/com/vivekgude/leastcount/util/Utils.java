package com.vivekgude.leastcount.util;

import com.vivekgude.leastcount.enums.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {

    public static String generateString(int size) {
        String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";
        StringBuilder s = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            int index = (int) (alphaNumericString.length() * Math.random());
            s.append(alphaNumericString.charAt(index));
        }
        return s.toString();
    }

    public static List<String> generateShuffledDecks(int numberOfDecks) {
        String[] suits = { Suit.HEART.toString(), Suit.SPADE.toString(), Suit.DIAMOND.toString(),
                Suit.CLUB.toString() };

        List<String> deck = new ArrayList<>();

        for (int deckCount = 0; deckCount < numberOfDecks; deckCount++) {
            for (String suit : suits) {
                for (int rank = 1; rank <= 13; rank++) {
                    deck.add(rank + suit);
                }
            }
        }

        Collections.shuffle(deck);
        return deck;
    }

    public static int computeHandTotal(List<String> cards) {
        if (cards == null || cards.isEmpty()) return 0;
        int total = 0;
        for (String code : cards) {
            if (code == null || code.isEmpty()) continue;
            String rankPart = code.substring(0, code.length() - 1);
            try {
                int rank = Integer.parseInt(rankPart);
                // Apply proper card values: A=1, J=11, Q=12, K=13, others face value
                if (rank == 1) {
                    total += 1; // Ace = 1
                } else if (rank == 11) {
                    total += 11; // Jack = 11
                } else if (rank == 12) {
                    total += 12; // Queen = 12
                } else if (rank == 13) {
                    total += 13; // King = 13
                } else {
                    total += rank; // 2-10 use face value
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return total;
    }

    // Optionally: helper to extract rank string (not used externally)
    static String extractRank(String code) {
        if (code == null || code.isEmpty()) return "";
        return code.substring(0, code.length() - 1);
    }

    /**
     * Generate two standard decks combined and shuffled
     */
    public static List<String> generateTwoDecksShuffled() {
        return generateShuffledDecks(2);
    }

    /**
     * Check if all cards in a list have the same rank
     */
    public static boolean allSameRank(List<String> cards) {
        if (cards == null || cards.isEmpty()) return false;
        if (cards.size() == 1) return true;
        
        String firstRank = extractRank(cards.get(0));
        return cards.stream()
                .map(Utils::extractRank)
                .allMatch(rank -> rank.equals(firstRank));
    }

}