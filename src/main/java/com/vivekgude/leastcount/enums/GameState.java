package com.vivekgude.leastcount.enums;

public enum GameState {

    INVALID(0), WAITING(10), STARTING(20), INPROGRESS(30), COMPLETED(40);

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
