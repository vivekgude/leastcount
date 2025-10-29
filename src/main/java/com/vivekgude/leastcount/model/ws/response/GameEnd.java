package com.vivekgude.leastcount.model.ws.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GameEnd {
    private String type;
    private long winnerId;
    private List<Map<String, Object>> finalScores;
}