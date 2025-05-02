package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.model.WebSocketMessage;

public interface MessageHandler {
    void handleMessage(String gameId, WebSocketMessage message);
    String getMessageType();
} 