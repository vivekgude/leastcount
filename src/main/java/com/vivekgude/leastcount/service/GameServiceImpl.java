package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.model.dto.GameDTO;
import com.vivekgude.leastcount.model.dto.GameConfig;
import com.vivekgude.leastcount.model.dto.UserDataDTO;
import com.vivekgude.leastcount.model.ws.response.GameDetailsRes;
import com.vivekgude.leastcount.redis.GameCache;
import com.vivekgude.leastcount.redis.PlayerCache;
import com.vivekgude.leastcount.util.Utils;
import com.vivekgude.leastcount.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.vivekgude.leastcount.constants.Constants.*;
import static com.vivekgude.leastcount.redis.GameCache.*;

@Slf4j
@Service
public class GameServiceImpl implements GameService {

    private final GameCache gameCache;
    private final PlayerCache playerCache;

    public GameServiceImpl(GameCache gameCache, PlayerCache playerCache) {
        this.gameCache = gameCache;
        this.playerCache = playerCache;
    }

    @Override
    public GameDTO createGame(long userId, String userName, GameConfig overrides) {
        String gameId = Utils.generateString(GAME_ID_SIZE);
        gameCache.addFieldToMap(GAME + gameId, STATE, GameState.WAITING.toString());
        gameCache.addFieldToMap(GAME + gameId, HOST, String.valueOf(userId));
        gameCache.addFieldToMap(GAME + gameId, HOST_NAME, userName);
        gameCache.addValuesInSet(JOINED_PLAYERS + gameId, String.valueOf(userId));
        playerCache.addPlayerName(gameId, userId, userName);

        // Persist per-game overrides if provided
        if (overrides != null) {
            if (overrides.getCardsPerPlayer() != null) {
                gameCache.addFieldToMap(GAME + gameId, CARDS_PER_PLAYER, String.valueOf(overrides.getCardsPerPlayer()));
            }
            if (overrides.getExitScore() != null) {
                gameCache.addFieldToMap(GAME + gameId, EXIT_SCORE, String.valueOf(overrides.getExitScore()));
            }
            if (overrides.getInvalidDeclarationPenalty() != null) {
                gameCache.addFieldToMap(GAME + gameId, INVALID_PENALTY, String.valueOf(overrides.getInvalidDeclarationPenalty()));
            }
            if (overrides.getMoveTimeMs() != null) {
                gameCache.addFieldToMap(GAME + gameId, MOVE_TIME_CONFIG, String.valueOf(overrides.getMoveTimeMs()));
            }
        }
        // initialize round number for new game
        gameCache.addFieldToMap(GAME + gameId, ROUND_NO, String.valueOf(1));
        UserDataDTO userDataDTO = new UserDataDTO(userId, userName);
        return new GameDTO(gameId, userDataDTO, Collections.singletonList(userDataDTO));
    }

    @Override
    public Optional<GameDTO> joinGame(long userId, String userName, String gameId) {
        int gameState = gameCache.getGameState(gameId);
        if (gameState == GameState.INVALID.getType()) {
            return Optional.empty();
        } else if (gameState == GameState.WAITING.getType()) {
            List<Long> joinedPlayers = gameCache.getJoinedPlayers(gameId);
            if (!joinedPlayers.contains(userId)) {
                // Add player to game
                gameCache.addValuesInSet(JOINED_PLAYERS + gameId, String.valueOf(userId));
                playerCache.addPlayerName(gameId, userId, userName);
                
                // Get updated player list for response
                List<Long> updatedPlayers = gameCache.getJoinedPlayers(gameId);
                List<UserDataDTO> playersDetails = new ArrayList<>();
                for (long playerId : updatedPlayers) {
                    String playerName = playerCache.getPlayerName(gameId, playerId);
                    playersDetails.add(new UserDataDTO(playerId, playerName));
                }
                
                // Create response DTO
                long host = Long.parseLong(gameCache.getFieldInMap(GAME + gameId, HOST));
                String hostName = gameCache.getFieldInMap(GAME + gameId, HOST_NAME);
                UserDataDTO userDataDTO = new UserDataDTO(host, hostName);
                GameDTO gameDTO = new GameDTO(gameId, userDataDTO, playersDetails);
                
                // Broadcast updated game details to all players
                try {
                    GameDetailsRes gameDetailsRes = new GameDetailsRes();
                    gameDetailsRes.setType("gamedetailsres");
                    gameDetailsRes.setGameState(gameState);
                    gameDetailsRes.setHost(userDataDTO);
                    gameDetailsRes.setPlayers(playersDetails);
                    gameDetailsRes.setCurrentPlayer(0); // No current player in waiting state
                    gameDetailsRes.setMoveTime(0); // No move time in waiting state
                    
                    WebSocketUtil.broadcastToGame(gameId, gameDetailsRes);
                    log.info("Broadcasted game details after player {} joined game {}", userName, gameId);
                } catch (Exception e) {
                    log.error("Failed to broadcast game details after player join: {}", e.getMessage(), e);
                }
                
                return Optional.of(gameDTO);
            }
            return Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean exitGame(long userId, String userName, String gameId) {
        int gameState = gameCache.getGameState(gameId);
        if (gameState == GameState.WAITING.getType()) {
            List<Long> joinedPlayers = gameCache.getJoinedPlayers(gameId);
            if (joinedPlayers.contains(userId)) {
                // Remove player from game
                gameCache.removeValuesInSet(JOINED_PLAYERS + gameId, String.valueOf(userId));
                playerCache.deleteKeys(PLAYER + userId + ":" + GAME + gameId);
                
                // Broadcast updated game details after player exit
                try {
                    List<Long> updatedPlayers = gameCache.getJoinedPlayers(gameId);
                    List<UserDataDTO> playersDetails = new ArrayList<>();
                    for (long playerId : updatedPlayers) {
                        String playerName = playerCache.getPlayerName(gameId, playerId);
                        playersDetails.add(new UserDataDTO(playerId, playerName));
                    }
                    
                    // Get host information
                    long hostId = Long.parseLong(gameCache.getFieldInMap(GAME + gameId, HOST));
                    String hostName = gameCache.getFieldInMap(GAME + gameId, HOST_NAME);
                    UserDataDTO hostData = new UserDataDTO(hostId, hostName);
                    
                    GameDetailsRes gameDetailsRes = new GameDetailsRes();
                    gameDetailsRes.setType("gamedetailsres");
                    gameDetailsRes.setGameState(gameState);
                    gameDetailsRes.setHost(hostData);
                    gameDetailsRes.setPlayers(playersDetails);
                    gameDetailsRes.setCurrentPlayer(0);
                    gameDetailsRes.setMoveTime(0);
                    
                    WebSocketUtil.broadcastToGame(gameId, gameDetailsRes);
                    log.info("Broadcasted game details after player {} exited game {}", userName, gameId);
                } catch (Exception e) {
                    log.error("Failed to broadcast game details after player exit: {}", e.getMessage(), e);
                }
                
                return true;
            }
        }
        return false;
    }

}
