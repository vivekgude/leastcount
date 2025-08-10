package com.vivekgude.leastcount.model.ws.response;

import com.vivekgude.leastcount.model.ws.WebSocketRes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoundEnd extends WebSocketRes {
    private long winnerId;     // -1 if invalid declaration
    private int winnerTotal;   // -1 if invalid declaration
    private List<Map<String, Object>> perPlayerAdded; // [{playerId, added}]
    private List<Map<String, Object>> gameScores;     // [{playerId, total}]
}