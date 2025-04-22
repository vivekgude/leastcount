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

}