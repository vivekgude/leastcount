package com.vivekgude.leastcount.util;

import com.google.gson.Gson;
import com.vivekgude.leastcount.model.WebSocketMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketUtil {
    private static final Gson gson = new Gson();
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public static void addSession(String gameId, WebSocketSession session) {
        sessions.put(gameId, session);
    }

    public static void removeSession(String gameId) {
        sessions.remove(gameId);
    }

    public static void sendMessage(String gameId, WebSocketMessage message) throws IOException {
        WebSocketSession session = sessions.get(gameId);
        if (session != null && session.isOpen()) {
            String jsonMessage = gson.toJson(message);
            session.sendMessage(new TextMessage(jsonMessage));
        }
    }

    public static void broadcastToGame(String gameId, WebSocketMessage message) throws IOException {
        //TODO: fix broadcast logic
        WebSocketSession session = sessions.get(gameId);
        if (session != null && session.isOpen()) {
            String jsonMessage = gson.toJson(message);
            session.sendMessage(new TextMessage(jsonMessage));
        }
    }
} 