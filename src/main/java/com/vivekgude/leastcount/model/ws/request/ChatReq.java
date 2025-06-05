package com.vivekgude.leastcount.model.ws.request;

import com.vivekgude.leastcount.model.ws.WebSocketReq;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReq extends WebSocketReq {
    private String message;
}
