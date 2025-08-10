## LeastCount Backend — Step-by-step Changes (per RULES)

This document outlines the required backend work to implement the finalized rules. It includes configuration, Redis schema, WebSocket handlers, timers, and testing.

### 1) Configuration and constants
- Defaults are hardcoded in `Constants`:
  - `DEFAULT_CARDS_PER_PLAYER = 5`
  - `DEFAULT_EXIT_SCORE = 100`
  - `DEFAULT_INVALID_DECLARATION_PENALTY = 40`
  - `DEFAULT_MOVE_TIME_MS = 30000`
- Accept per-game overrides on creation via `GameConfig` (request body fields: `cardsPerPlayer`, `exitScore`, `invalidDeclarationPenalty`, `moveTimeMs`). Persist them in `game:{gameId}`.

### 2) Redis schema extensions
- Hash `game:{gameId}` add fields:
  - `cardsPerPlayer`, `exitScore`, `invalidPenalty`, `roundNo`, `currentPlayer`, `moveTime`
- New keys per game:
  - `open:{gameId}` → List of cards in the current open pile (cards dropped by previous player)
  - `deck:{gameId}` → List representing closed pile (top at tail)
  - `eliminated:{gameId}` → Set of eliminated playerIds
  - `gameScore:{gameId}` → Hash of cumulative score per playerId
- Keep existing:
  - `joinedPlayers:{gameId}` (Set)
  - `player:{gameId}:{playerId}playerCards` (List) — player’s hand
  - `player:{playerId}:game:{gameId}` (Hash) — player metadata

### 3) Utility services
- `Utils`:
  - `generateTwoDecksShuffled(): List<String>` — two standard decks combined and shuffled
  - `computeHandTotal(List<String>): int` — A=1, J=11, Q=12, K=13; others face value
  - `rebuildClosedPile(gameId)` — recompute closed pile by subtracting all players’ hands and current open pile from full two-deck set; shuffle remainder into `deck:{gameId}`
- `PlayerCache`:
  - Helper to fetch `getAllPlayerIds(gameId)` from `joinedPlayers:{gameId}`
  - Helpers to iterate/get each player’s cards for rebuild computation
- `GameCache`:
  - Typed getters/setters for new fields (cardsPerPlayer, exitScore, invalidPenalty, roundNo)
  - Helpers to manage `open:{gameId}` and `deck:{gameId}` (push/pop/getAll/clear)
  - Helpers for `gameScore:{gameId}` and `eliminated:{gameId}`
- Optional `DeckService` to encapsulate drawing logic and auto-rebuild.

### 4) Start game flow
- Update `StartGameHandler`:
  - Enforce host-only start and min 2 players
  - Resolve `cardsPerPlayer` from `game:{id}` or default
  - Build two-deck shuffled list, deal N to each player
  - Initialize `open:{id}` empty and `deck:{id}` with remaining cards
  - Initialize `gameScore:{id}` if absent; set `roundNo = 1` (or increment for next round)
  - Set `currentPlayer`, `moveTime = now + (per-game moveTimeMs or default)`
  - Schedule `TurnTimerJob`
  - Broadcast a state snapshot (see §7)

### 5) WebSocket messages & payloads
- Requests (new):
  - `dropreq` → `{ cards: string[] }`
  - `pickreq` → `{ source: "open"|"closed", card?: string }` (card required when source=open)
  - `showreq` → `{}`
- Responses/broadcasts (new, typed DTOs):
  - `dropres` → `{ playerId, open: string[], deckCount }`
  - `pickres` → `{ playerId, source, card?: string, open: string[] }`
  - `stateupdate` → `{ currentPlayer, moveTime, open, deckCount, gameScores?, eliminated?, roundNo? }`
  - `roundend` → `{ winnerId, winnerTotal, perPlayerAdded: Array<{playerId, added}>, gameScores: Array<{playerId, total}> }`
  - `gameend` → `{ winnerId, finalScores: Array<{playerId, total}> }`
  - Keep: `cardsres` (private hand to player), `gamestartres`

Examples

```json
// dropreq
{ "type": "dropreq", "gameId": "G1", "userId": 101, "content": "{\"cards\":[\"7H\",\"7S\"]}" }

// dropres
{ "type": "dropres", "playerId": 101, "open": ["7H","7S"], "deckCount": 73 }

// pickreq (open)
{ "type": "pickreq", "gameId": "G1", "userId": 101, "content": "{\"source\":\"open\",\"card\":\"7H\"}" }

// pickres (open)
{ "type": "pickres", "playerId": 101, "source": "open", "card": "7H", "open": ["7S"] }

// pickreq (closed)
{ "type": "pickreq", "gameId": "G1", "userId": 101, "content": "{\"source\":\"closed\"}" }

// pickres (closed)
{ "type": "pickres", "playerId": 101, "source": "closed", "card": "", "open": ["7H","7S"] }

// stateupdate
{ "type": "stateupdate", "currentPlayer": 102, "moveTime": 1735689600000, "open": ["7S"], "deckCount": 72,
  "gameScores": {"101":"10","102":"4"}, "eliminated": [103], "roundNo": 2 }

// roundend
{ "type": "roundend", "winnerId": 102, "winnerTotal": 7,
  "perPlayerAdded": [{"playerId":101, "added": 3},{"playerId":102, "added":0}],
  "gameScores": [{"playerId":101, "total": 13},{"playerId":102, "total": 4}] }

// gameend
{ "type": "gameend", "winnerId": 102,
  "finalScores": [{"playerId":101, "total": 99},{"playerId":102, "total": 42}] }

// errorres
{ "type": "errorres", "code": "not_your_turn", "messageText": "Not your turn", "receiver": 101 }
```

### 6) Handlers and validation
- `DropHandler` (new):
  - Validate: game in progress, it’s player’s turn, within timer, ownership of all `cards`
  - Baseline rule: all dropped cards in a turn must share the same rank
  - Remove dropped cards from player’s hand; set `open:{id}` to dropped set (replacing previous open pile)
  - Broadcast `dropres` + `stateupdate`
- `PickHandler` (new):
  - Validate: same turn as the drop; player must pick exactly one card
  - If `open`: ensure `card` exists in `open:{id}`, remove it from `open:{id}`
  - If `closed`: pop from `deck:{id}`; when empty, call `rebuildClosedPile` then pop
  - Add picked card to player hand
  - Advance turn: set `currentPlayer` → next active (skip eliminated), set `moveTime`, delete and reschedule `TurnTimerJob`
  - Broadcast `pickres` + `stateupdate` (include remaining open and deckCount)
- `ShowHandler` (new):
  - Validate: it’s player’s turn
  - Compute per-player `S(p)` totals
  - If valid declaration (S(declarer) ≤ S(others)): winner gets 0; others gain `S(p) - S(winner)`
  - Else invalid: declarer gains `invalidPenalty + (S(declarer) - min S(p))`; others gain 0
  - Update `gameScore:{id}`, eliminate players with score ≥ exitScore (add to `eliminated:{id}`)
  - Broadcast `roundend` with per-player added and new totals
  - If only one active remains → broadcast `gameend`; else start next round (re-run start flow with `roundNo++`)

### 7) State sync & snapshots
- On connect/`gamedetailsreq`, send a snapshot containing:
  - Players (id/name), `currentPlayer`, `moveTime`, `open` (array), `deckCount`
  - For the requesting player only: `cards`
  - `gameScores`, `roundNo`, `eliminated`
- Consider consolidating around `stateupdate` + private `cardsres` for consistency.

### 8) Timers and jobs
- `TurnTimerJob`:
  - On expiry: advance to next active player (skip eliminated), set `moveTime`, broadcast `stateupdate`
  - Do not mutate hands or `open:{id}`
- Always delete existing job key `turnTimer_{gameId}` before scheduling the next one.

- `NextRoundJob` (new):
  - Scheduled by `ShowHandler` 10s after `roundend` if multiple active players remain
  - Re-deals to active players, resets `open:{id}`/`deck:{id}`, sets `INPROGRESS`, selects first player, computes `moveTime`, broadcasts `gamestartres`, per-player `cardsres`, broadcasts `scoreres`, and schedules next `TurnTimerJob` and `InitialPlayerMoveJob`.

-### 9) REST/API adjustments
- `POST /api/game/createGame`: accept optional config overrides per game in `GameConfig` (`cardsPerPlayer`, `exitScore`, `invalidDeclarationPenalty`, `moveTimeMs`); validate and store in `game:{id}`
- Enforce host-only `startgamereq` in `StartGameHandler` (restore checks)

### 10) Security and validation
- Strictly validate WS payloads (types, ownership, duplicates, presence)
- Guard against actions by eliminated players or when game not in progress
- Robust error logging; optionally emit `errorres` to clients

### 11) Testing
- Unit tests:
  - Drop validation (same-rank multi-drop)
  - Pick from open by explicit card; deck rebuild on empty closed pile
  - Declaration valid/invalid scoring; elimination at exitScore
  - Turn rotation skipping eliminated players
- Integration tests:
  - Full turn: drop → pick → rotate
  - Round end → next round; game end flow
  - Timer skip advancing turns

### 12) Migration & cleanup
- Replace old per-move scoring in `PlayCardsHandler` once new drop/pick/show logic is live
- Prefer `gameScore:{gameId}` for cumulative scores; keep any per-round temp values separate if needed

### Implementation order (recommended)
1. Config + Redis schema + utilities (Sections 1–3)
2. Start game updates (4)
3. New WS handlers: drop → pick → show (6)
4. State snapshot and `stateupdate` broadcasting (7)
5. Timer integration and turn rotation (8)
6. REST config overrides (9)
7. Cleanup old scoring + tests (10–12)


### Appendix — Redis list semantics for closed deck

- We use a Redis List to model the closed pile with the TOP of the deck at the tail of the list.
- We push the initial Java list into Redis using LPUSH in original order and draw using RPOP.
- This maintains draw order identical to the original Java list order.

Per-code reference: see `GameCache.setDeck`, `GameCache.popFromDeck`.

### Appendix — Per-game configuration keys

- `game:{id}` hash fields:
  - `cardsPerPlayer`, `exitScore`, `invalidPenalty`, `roundNo`, `currentPlayer`, `moveTime`
  - `moveTimeConfigMs` (per-game move duration override). When absent, default is 30000ms.


