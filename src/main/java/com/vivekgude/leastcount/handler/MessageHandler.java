package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.model.ws.WebSocketReq;

public interface MessageHandler {
    void handleMessage(String gameId, WebSocketReq message);
    String getMessageType();
} 