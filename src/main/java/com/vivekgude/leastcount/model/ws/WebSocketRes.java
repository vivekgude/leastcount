package com.vivekgude.leastcount.model.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketRes {
    private String type;
    private String content;
    private long receiver;
}
