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
@EqualsAndHashCode(callSuper = true)
public class CardsRes extends WebSocketRes {
    private String gameId;
    private List<String> cards;
} 