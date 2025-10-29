package com.vivekgude.leastcount.model.dto;

import lombok.Data;

@Data
public class GameConfig {
    private Integer cardsPerPlayer;
    private Integer exitScore;
    private Integer invalidDeclarationPenalty;
    private Long moveTimeMs;
    
    // Constructor with defaults
    public GameConfig() {
        // Defaults will be applied when creating games
    }
}