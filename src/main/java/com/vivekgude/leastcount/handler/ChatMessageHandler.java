package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.model.WebSocketMessage;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.vivekgude.leastcount.constants.MessageType.*;

@Component
@Slf4j
public class ChatMessageHandler implements MessageHandler {
    @Override
    public void handleMessage(String gameId, WebSocketMessage message) {
        try {
            log.info("Handling chat message from {}: {}", message.getSender(), message.getContent());

            String processedContent = "Processed: " + message.getContent();

            // Create a response message
            WebSocketMessage response = new WebSocketMessage(CHAT, processedContent, message.getSender());

            // Send the response back to the client
            WebSocketUtil.sendMessage(gameId, response);

            // You can also broadcast to all clients in the same game
            WebSocketMessage broadcast = new WebSocketMessage(CHAT, processedContent, message.getSender());
            WebSocketUtil.broadcastToGame(gameId, broadcast);

        } catch (Exception e) {
            log.error("Error sending chat message: {}", e.getMessage());
        }
    }

    @Override
    public String getMessageType() {
        return "chat";
    }
} 