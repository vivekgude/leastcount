package com.vivekgude.leastcount.model.ws.response;

import com.vivekgude.leastcount.model.ws.WebSocketRes;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatRes extends WebSocketRes {
    private long senderId;
    private String senderName;
    private String text;
    private long ts;

    public ChatRes() {
        super("chatres", null, 0L);
    }
}
