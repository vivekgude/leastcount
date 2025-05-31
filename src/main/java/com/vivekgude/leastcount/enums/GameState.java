package com.vivekgude.leastcount.enums;

public enum GameState {

    INVALID(0), WAITING(10), INPROGRESS(20), COMPLETED(30);

    private final int type;

    GameState(int state) {
        this.type = state;
    }

    public int getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return String.valueOf(this.type);
    }

}
