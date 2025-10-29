package com.vivekgude.leastcount.model.ws.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RoundEnd {
    private String type;
    private long winnerId;
    private int winnerTotal;
    private List<Map<String, Object>> perPlayerAdded;
    private List<Map<String, Object>> gameScores;
}