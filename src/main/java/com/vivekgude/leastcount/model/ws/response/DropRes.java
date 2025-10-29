package com.vivekgude.leastcount.model.ws.response;

import lombok.Data;

import java.util.List;

@Data
public class DropRes {
    private String type;
    private long playerId;
    private List<String> open;
    private int deckCount;
}