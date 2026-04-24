package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.model.ws.response.ChatRes;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ChatMessageHandler implements MessageHandler {

    private static final int MAX_CHAT_LENGTH = 500;

    @Override
    public void handleMessage(WebSocketReq message) {
        try {
            String gameId = message.getGameId();
            if (gameId == null || gameId.isBlank()) return;

            String raw = message.getContent();
            if (raw == null) return;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) return;
            if (trimmed.length() > MAX_CHAT_LENGTH) {
                trimmed = trimmed.substring(0, MAX_CHAT_LENGTH);
            }
            String safe = sanitize(trimmed);

            ChatRes response = new ChatRes();
            response.setSenderId(message.getUserId());
            response.setSenderName(message.getUsername());
            response.setText(safe);
            response.setTs(System.currentTimeMillis());

            WebSocketUtil.broadcastToGame(gameId, response);
        } catch (Exception e) {
            log.error("Error handling chat message: {}", e.getMessage(), e);
        }
    }

    private static String sanitize(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Override
    public String getMessageType() {
        return "chatreq";
    }
}
