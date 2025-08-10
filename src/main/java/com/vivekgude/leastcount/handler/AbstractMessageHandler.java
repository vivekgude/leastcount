package com.vivekgude.leastcount.handler;

import com.google.gson.Gson;
import com.vivekgude.leastcount.model.ws.response.ErrorRes;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMessageHandler {

    private final Gson gson = new Gson();

    protected void sendError(String gameId, long userId, String code, String message) {
        try {
            ErrorRes err = new ErrorRes();
            err.setType("errorres");
            err.setCode(code);
            err.setMessageText(message);
            err.setReceiver(userId);
            WebSocketUtil.sendMessage(gameId, userId, err);
        } catch (Exception e) {
            log.error("Unable to send errormsg {}", message);
        }
    }
}
