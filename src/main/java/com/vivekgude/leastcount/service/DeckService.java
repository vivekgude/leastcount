package com.vivekgude.leastcount.service;

public interface DeckService {
    String drawFromClosed(String gameId);
    int getClosedCount(String gameId);
}


