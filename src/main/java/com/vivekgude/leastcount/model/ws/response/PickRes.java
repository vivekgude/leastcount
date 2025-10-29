package com.vivekgude.leastcount.model.ws.response;

import lombok.Data;

import java.util.List;

@Data
public class PickRes {
    private String type;
    private long playerId;
    private String source;
    private String card;
    private List<String> open;
}