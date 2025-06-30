package com.vivekgude.leastcount.model.ws.response;

import com.vivekgude.leastcount.model.ws.WebSocketRes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreRes extends WebSocketRes {
    private List<Score> scores;
}
