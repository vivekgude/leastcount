package com.vivekgude.leastcount.controller;

import com.vivekgude.leastcount.model.dto.GameDTO;
import com.vivekgude.leastcount.service.GameService;
import com.vivekgude.leastcount.util.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.Optional;

import static com.vivekgude.leastcount.constants.Constants.*;

@Slf4j
public class GameController extends BaseController {

    @Autowired
    GameService gameService;

    @PostMapping("/createGame")
    public ResponseEntity createGame(@RequestBody Map<String, String> req) {
        //TODO: change the RequestBody to POJO
        log.info(req.toString());
        GameDTO gameDetails = gameService.createGame(Integer.parseInt(req.get("userId")),
                req.get("username"));

        return ResponseEntity.ok(ResponseUtil.success(GAME_CREATED, gameDetails));
    }

    @PostMapping("/joinGame")
    public ResponseEntity joinGame(@RequestBody Map<String, String> req) {
        //TODO: change the RequestBody to POJO
        Optional<GameDTO> gameDetails = gameService.joinGame(Integer.parseInt(req.get("userId")),
                req.get("username"), req.get("gameId"));

        if (gameDetails.isPresent()) {
            return ResponseEntity.ok(ResponseUtil.success(JOINED_GAME, gameDetails));
        } else {
            return ResponseEntity.ok(ResponseUtil.error(JOINED_GAME_FAILED));
        }
    }

}
