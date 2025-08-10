package com.vivekgude.leastcount.model.dto;

import lombok.Data;

@Data
public class GameConfig {
    private Integer cardsPerPlayer;
    private Integer exitScore;
    private Integer invalidDeclarationPenalty;
    private Long moveTimeMs;
}