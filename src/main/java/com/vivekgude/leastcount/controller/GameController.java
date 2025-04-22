package com.vivekgude.leastcount.controller;

import com.vivekgude.leastcount.model.dto.GameDTO;
import com.vivekgude.leastcount.service.GameService;
import com.vivekgude.leastcount.util.ResponseUtil;
import com.vivekgude.leastcount.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

import static com.vivekgude.leastcount.Constants.*;

public class GameController extends BaseController {

    @Autowired
    GameService gameService;

    @PostMapping("/createGame")
    public ResponseEntity createGame(@RequestBody Map<String, String> req) {
        //TODO: change the RequestBody to POJO
        GameDTO gameDetails = gameService.createGame(Integer.parseInt(req.get("userId")),
                req.get("username"));
        return ResponseEntity.ok(ResponseUtil.success(true, GAME_CREATED, gameDetails));
    }

    @PostMapping("/joinGame")
    public ResponseEntity joinGame(@RequestBody Map<String, String> req) {
        //TODO: change the RequestBody to POJO
        GameDTO gameDetails = gameService.createGame(Integer.parseInt(req.get("userId")),
                req.get("username"));
        return ResponseEntity.ok(ResponseUtil.success(true, GAME_CREATED, gameDetails));
    }

}
