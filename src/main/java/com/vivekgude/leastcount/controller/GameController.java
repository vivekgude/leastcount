package com.vivekgude.leastcount.controller;

import com.vivekgude.leastcount.model.dto.GameDTO;
import com.vivekgude.leastcount.service.GameService;
import com.vivekgude.leastcount.util.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

import static com.vivekgude.leastcount.constants.Constants.*;

@Slf4j
@RestController
@RequestMapping("/api/game")
public class GameController extends BaseController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/createGame")
    public ResponseEntity createGame(@AuthenticationPrincipal User user, @RequestBody Map<String, String> req) {
        //TODO: change the RequestBody to POJO
        GameDTO gameDetails = gameService.createGame(Long.parseLong(req.get("userId")),
                req.get("username"));

        return ResponseEntity.ok(ResponseUtil.success(GAME_CREATED, gameDetails));
    }

    @PostMapping("/joinGame")
    public ResponseEntity joinGame(@RequestBody Map<String, String> req) {
        //TODO: change the RequestBody to POJO
        Optional<GameDTO> gameDetails = gameService.joinGame(Long.parseLong(req.get("userId")),
                req.get("username"), req.get("gameId"));

        if (gameDetails.isPresent()) {
            return ResponseEntity.ok(ResponseUtil.success(JOINED_GAME, gameDetails));
        } else {
            return ResponseEntity.ok(ResponseUtil.error(JOINED_GAME_FAILED));
        }
    }

}
