package com.vivekgude.leastcount.controller;

import com.vivekgude.leastcount.model.dto.GameDTO;
import com.vivekgude.leastcount.model.dto.GameConfig;
import com.vivekgude.leastcount.security.CustomUserDetails;
import com.vivekgude.leastcount.service.GameService;
import com.vivekgude.leastcount.util.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> createGame(@AuthenticationPrincipal CustomUserDetails user,
            @RequestBody(required = false) GameConfig request) {
        GameDTO gameDetails = gameService.createGame(user.getUserId(), user.getUsername(), request);

        return ResponseEntity.ok(ResponseUtil.success(GAME_CREATED, gameDetails));
    }

    @PostMapping("/joinGame/{gameId}")
    public ResponseEntity<?> joinGame(@AuthenticationPrincipal CustomUserDetails user,
            @PathVariable("gameId") String gameId) {
        //TODO: change the RequestBody to POJO
        Optional<GameDTO> gameDetails = gameService.joinGame(user.getUserId(), user.getUsername(), gameId);

        if (gameDetails.isPresent()) {
            return ResponseEntity.ok(ResponseUtil.success(JOINED_GAME, gameDetails));
        } else {
            return ResponseEntity.ok(ResponseUtil.error(JOIN_GAME_FAILED));
        }
    }

    @PostMapping("/exitGame/{gameId}")
    public ResponseEntity<?> exitGame(@AuthenticationPrincipal CustomUserDetails user,
            @PathVariable("gameId") String gameId) {
        //TODO: change the RequestBody to POJO
        boolean isSuccess = gameService.exitGame(user.getUserId(), user.getUsername(), gameId);

        if (isSuccess) {
            return ResponseEntity.ok(ResponseUtil.success(EXITED_GAME, null));
        } else {
            return ResponseEntity.ok(ResponseUtil.error(EXIT_GAME_FAILED));
        }
    }

}
