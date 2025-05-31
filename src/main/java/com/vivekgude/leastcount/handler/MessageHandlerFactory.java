package com.vivekgude.leastcount.handler;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MessageHandlerFactory {

    private final Map<String, MessageHandler> handlerMap = new HashMap<>();

    public MessageHandlerFactory(List<MessageHandler> handlers) {
        for (MessageHandler handler : handlers) {
            handlerMap.put(handler.getMessageType(), handler);
        }
    }

    public MessageHandler getHandler(String messageType) {
        MessageHandler handler = handlerMap.get(messageType);
        if (handler == null) {
            throw new IllegalArgumentException("No handler found for message type: " + messageType);
        }
        return handler;
    }
}

