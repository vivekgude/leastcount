package com.vivekgude.leastcount.handler;

import com.google.gson.Gson;
import com.vivekgude.leastcount.model.ws.WebSocketReq;
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

import static com.vivekgude.leastcount.constants.Constants.*;

@Component
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

    private final Gson gson = new Gson();

    @Autowired
    private MessageHandlerFactory messageHandlerFactory;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String gameId = getGameId(session);
        long userId = (Long) session.getAttributes().get(USERID);
        String username = (String) session.getAttributes().get(USERNAME);
        
        if (gameId != null && userId != 0) {

            WebSocketUtil.addSession(gameId, userId, session);
            log.info("New WebSocket connection established for gameId:{} userId:{} username:{}", gameId, userId, username);
        } else {
            log.debug("Connection rejected: Missing gameId or userId");
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
        long userId = (Long) session.getAttributes().get(USERID);
        
        if (gameId != null) {
            WebSocketUtil.removeSession(gameId, userId);
            log.info("WebSocket connection closed for gameId:{} userId:{} status:{}", gameId, userId, status);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String gameId = getGameId(session);
            long userId = (Long) session.getAttributes().get(USERID);
            String username = (String) session.getAttributes().get(USERNAME);
            
            log.info("Received message for gameId:{} userId:{} username:{} message:{}", gameId, userId, username, message.getPayload());

            WebSocketReq webSocketReq = gson.fromJson(message.getPayload(), WebSocketReq.class);
            webSocketReq.setUserId(userId); // Add userId to the message
            webSocketReq.setUsername(username); // Add username to the message
            webSocketReq.setGameId(gameId); // Add gameId to the message

            MessageHandler handler = messageHandlerFactory.getHandler(webSocketReq.getType());
            handler.handleMessage(webSocketReq);

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
        }
    }

    private String getGameId(WebSocketSession session) {
        UriComponents uriComponents = UriComponentsBuilder.fromUri(session.getUri()).build();
        return uriComponents.getQueryParams().getFirst("gameId");
    }
}