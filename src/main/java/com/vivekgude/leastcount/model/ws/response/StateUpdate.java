package com.vivekgude.leastcount.model.ws.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StateUpdate {
    private String type;
    private long currentPlayer;
    private long moveTime;
    private List<String> open;
    private int deckCount;
    private Map<String, String> gameScores;
    private List<Long> eliminated;
    private Integer roundNo;
}