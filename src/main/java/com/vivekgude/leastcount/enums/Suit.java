package com.vivekgude.leastcount.enums;

public enum Suit {

    HEART("h"), SPADE("s"), DIAMOND("d"), CLUB("c");

    private final String type;

    Suit(String suit) {
        type = suit;
    }

    public boolean equalsName(String otherName) {
        return type.equals(otherName);
    }

    public String toString() {
        return this.type;
    }
}
