package com.vivekgude.leastcount.enums;

public enum WebSocketRes {

    GAME_DETAILS_RES("gamedetailsres");

    private final String type;

    WebSocketRes(String type) {
        this.type = type;
    }

    public boolean equalsName(String type) {
        return this.type.equals(type);
    }

    public String toString() {
        return this.type;
    }
}
