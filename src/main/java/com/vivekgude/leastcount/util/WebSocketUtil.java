package com.vivekgude.leastcount.util;

import com.google.gson.Gson;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WebSocketUtil {
    private static final Gson gson = new Gson();
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * Creates a unique key for a player session using gameId and userId
     */
    private static String createSessionKey(String gameId, long userId) {
        return gameId + ":" + userId;
    }

    public static void addSession(String gameId, long userId, WebSocketSession session) {
        String sessionKey = createSessionKey(gameId, userId);
        sessions.put(sessionKey, session);
    }

    public static void removeSession(String gameId, long userId) {
        String sessionKey = createSessionKey(gameId, userId);
        sessions.remove(sessionKey);
    }

    public static void sendMessage(String gameId, long userId, Object message) throws IOException {
        String sessionKey = createSessionKey(gameId, userId);
        WebSocketSession session = sessions.get(sessionKey);
        if (session != null && session.isOpen()) {
            String jsonMessage = gson.toJson(message);
            session.sendMessage(new TextMessage(jsonMessage));
        }
    }

    public static void broadcastToGame(String gameId, Object message) throws IOException {
        // Send message to all players in the game
        sessions.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(gameId + ":"))
                .forEach(entry -> {
                    WebSocketSession session = entry.getValue();
                    if (session != null && session.isOpen()) {
                        try {
                            String jsonMessage = gson.toJson(message);
                            session.sendMessage(new TextMessage(jsonMessage));
                        } catch (IOException e) {
                            // Log error and continue with other sessions
                            System.err.println("Error sending message to session: " + e.getMessage());
                        }
                    }
                });
    }

    /**
     * Get all active sessions for a specific game
     */
    public static Map<String, WebSocketSession> getGameSessions(String gameId) {
        return sessions.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(gameId + ":"))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().substring(gameId.length() + 1), // Extract userId
                        Map.Entry::getValue
                ));
    }

    /**
     * Check if a specific player is connected to a game
     */
    public static boolean isPlayerConnected(String gameId, long userId) {
        String sessionKey = createSessionKey(gameId, userId);
        WebSocketSession session = sessions.get(sessionKey);
        return session != null && session.isOpen();
    }

    /**
     * Get the number of connected players in a game
     */
    public static int getConnectedPlayerCount(String gameId) {
        return (int) sessions.keySet().stream()
                .filter(key -> key.startsWith(gameId + ":"))
                .count();
    }
} 