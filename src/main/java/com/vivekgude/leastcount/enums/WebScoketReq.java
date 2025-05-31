package com.vivekgude.leastcount.enums;

public enum WebScoketReq {

    GAME_DETAILS_REQ("gamedetailsreq");

    private final String type;

    WebScoketReq(String type) {
        this.type = type;
    }

    public boolean equalsName(String type) {
        return this.type.equals(type);
    }

    public String toString() {
        return this.type;
    }
}
