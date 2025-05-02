package com.vivekgude.leastcount.handler;

import com.google.gson.Gson;
import com.vivekgude.leastcount.model.WebSocketMessage;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

    private final Gson gson = new Gson();

    @Autowired
    private MessageHandlerFactory messageHandlerFactory;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String gameId = getGameId(session);
        if (gameId != null) {
            WebSocketUtil.addSession(gameId, session);
            log.info("New WebSocket connection established for gameId: {}", gameId);
        } else {
            log.debug("Connection rejected: No gameId provided");
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (Exception e) {
                log.error("Error closing session: {}", e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String gameId = getGameId(session);
        if (gameId != null) {
            WebSocketUtil.removeSession(gameId);
            log.info("WebSocket connection closed for gameId: {}", gameId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String gameId = getGameId(session);
            log.info("Received message for gameId {}: {}", gameId, message.getPayload());

            WebSocketMessage webSocketMessage = gson.fromJson(message.getPayload(), WebSocketMessage.class);

            MessageHandler handler = messageHandlerFactory.getHandler(webSocketMessage.getType());
            handler.handleMessage(gameId, webSocketMessage);

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
        }
    }

    private String getGameId(WebSocketSession session) {
        UriComponents uriComponents = UriComponentsBuilder.fromUri(session.getUri()).build();
        return uriComponents.getQueryParams().getFirst("gameId");
    }
}