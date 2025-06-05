package com.vivekgude.leastcount.handler;

import com.vivekgude.leastcount.model.ws.WebSocketReq;
import com.vivekgude.leastcount.model.ws.WebSocketRes;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.vivekgude.leastcount.constants.MessageType.*;

@Component
@Slf4j
public class ChatMessageHandler implements MessageHandler {

    @Override
    public void handleMessage(WebSocketReq message) {
        try {
            String gameId = message.getGameId();

            log.info("Handling chat message from {}: {}", message.getUserId(), message.getContent());

            String processedContent = "Processed: " + message.getContent();

            // Create a response message
            WebSocketRes response = new WebSocketRes(CHAT, processedContent, message.getUserId());

            // Send the response back to the client
            WebSocketUtil.sendMessage(gameId, response);

            // You can also broadcast to all clients in the same game
//            WebSocketRes broadcast = new WebSocketRes(CHAT, processedContent, message.getUserId());
//            WebSocketUtil.broadcastToGame(gameId, broadcast);

        } catch (Exception e) {
            log.error("Error sending chat message: {}", e.getMessage());
        }
    }

    @Override
    public String getMessageType() {
        return "chatreq";
    }
} 