package com.vivekgude.leastcount.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class GameDTO {
    private String gameId;
//    private long startTime;
    private UserDataDTO host;
    private List<UserDataDTO> players;

//    public GameDTO(String gameId, long startTime, UserDataDTO host, List<UserDataDTO> players) {
//        this.gameId = gameId;
//        this.startTime = startTime;
//        this.host = host;
//        this.players = players;
//    }

    public GameDTO(String gameId, UserDataDTO host, List<UserDataDTO> players) {
        this.gameId = gameId;
        this.host = host;
        this.players = players;
    }
}
