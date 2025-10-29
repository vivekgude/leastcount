package com.vivekgude.leastcount.constants;

public class Constants {

    public static final String PONG = "PONG";

    public static final String USERNAME = "username";
    public static final String USERID = "userId";

    public static final String USER_CREATED = "User is created";
    public static final String GAME_CREATED = "Game is created";
    public static final String JOINED_GAME = "Joined Game";
    public static final String JOIN_GAME_FAILED = "Unable to join game";
    public static final String EXITED_GAME = "Exited game";
    public static final String EXIT_GAME_FAILED = "Unable to exit game";

    public static final int GAME_ID_SIZE = 5;
    public static final int DECK_SIZE = 2;
    public static final int DEFAULT_CARDS_PER_PLAYER = 5; // default cards per player
    public static final int DEFAULT_EXIT_SCORE = 100;
    public static final int DEFAULT_INVALID_DECLARATION_PENALTY = 40;
    public static final long DEFAULT_MOVE_TIME_MS = 30000; // default move timer: 30s
    public static final long NEXT_ROUND_DELAY_MS = 10000; // auto next-round delay: 10s
    
    // Per-game configuration field names
    public static final String CARDS_PER_PLAYER = "cardsPerPlayer";
    public static final String EXIT_SCORE = "exitScore";
    public static final String INVALID_DECLARATION_PENALTY = "invalidDeclarationPenalty";
    public static final String MOVE_TIME_MS = "moveTimeMs";

}
