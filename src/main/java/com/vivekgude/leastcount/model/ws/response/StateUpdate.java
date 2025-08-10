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
public class StateUpdate extends WebSocketRes {
    private long currentPlayer;
    private long moveTime;
    private List<String> open;
    private int deckCount;
    private Map<String, String> gameScores; // playerId->total
    private List<Long> eliminated;
    private Integer roundNo;
}