## LeastCount — Official Rules (Project Spec)

This document defines the gameplay used by the app. It reflects your latest specifications and highlights configurable settings.

### Configuration
- Cards per player: configurable (suggested default: 5)
- Decks in play: 2 × 52-card decks (total 104 cards)
- Exit score: configurable (default: 100)
- Invalid declaration penalty: configurable (suggested default: 40)
- Move timer per turn: configurable (default in codebase: 30s)

Note: All configuration values are per-game and can be provided by the creator when creating a game. If omitted, the defaults above are applied for that game.

### Card values
- A = 1, J = 11, Q = 12, K = 13; number cards use their face values (2–10)
- Suits have no effect on value (rank-only scoring)
- “Same card” means same rank (e.g., 7♥, 7♠, 7♦, 7♣)
- No wild cards

### Setup
1) Shuffle the two decks together
2) Deal N cards (configurable; suggested 5) to each player
3) Remaining cards form the closed pile (face-down)
4) The open pile starts empty; it will hold the set of cards dropped by the most recent player turn and is visible to all
5) Determine turn order (e.g., by join order); Player 1 starts

### Turn structure
On your turn, you must complete a single atomic action consisting of two steps:
1) Drop: Select one or more cards from your hand to drop to the open pile
   - All cards dropped in the same turn must satisfy the drop validity rules (see “Drop validity”)
   - The open pile becomes exactly the set of cards you just dropped (replacing any previous open pile)
2) Pick: Immediately pick exactly one card
   - From the open pile: pick any one card from the current open pile
   - Or from the closed pile: pick the top card
3) End turn: play passes to the next player

Notes
- You must drop first, then pick; both happen in the same turn
- You must drop at least one card on your turn
- There is no “skip pick” rule
- If the move timer expires before you complete your action, your turn is skipped and the next player begins their turn

### Open pile (public)
- Contains exactly the cards dropped by the most recent player
- Next player may pick any one card from this open pile (not limited to a top card)
- After the next player’s pick, whatever remains in the open pile stays there until replaced by the next drop

### Closed pile (hidden)
- Contains the shuffled remainder after the initial deal
- When empty, it must be replenished as follows:
  - Reshuffle all cards that are not currently in any player’s hand and not in the current open pile
  - The reshuffled cards become the new closed pile

### Declaration (ending a round)
- A player may declare only on their own turn (instead of performing a drop+pick)
- A declaration is valid if the declaring player’s hand total is less than or equal to every other player’s hand total at that moment
  - If valid: the declaring player wins the round
  - If invalid: the round still ends immediately, and penalties are applied (see below)

### Scoring
Let S(p) be the total value of player p’s hand at declaration time.

Valid declaration (winner W)
- Winner W gains 0 round points
- Each other player p gains (S(p) − S(W)) round points

Invalid declaration (declaring player D)
- Find the lowest hand total among all players: S(min) = min_p S(p)
- Declaring player D gains: invalidDeclarationPenalty + (S(D) − S(min))
- Assumption (to keep flow simple unless specified otherwise): all other players gain 0 round points in an invalid declaration
  - If you want other players to also gain points in this case, specify the formula and we’ll update the rule

Game score
- Each player’s cumulative game score is the sum of their round points across all rounds
- A player is eliminated when their game score ≥ Exit score (default: 100)
- The game ends when the second-last player is eliminated; the remaining player is the winner

### Validity rules
Turn validity
- Only the current player may act; actions must complete before the move timer expiry

Drop validity (initial)
- You must drop at least one card
- Additional/stricter drop conditions will be finalized later; for now, the baseline is:
  - All dropped cards in a single turn must be of the same rank (this can be relaxed/tightened once exact rules are confirmed)

Pick validity
- Exactly one card must be picked:
  - Either any one card from the current open pile
  - Or the top card from the closed pile

Declaration validity
- Only on the declaring player’s turn
- Valid if S(declarer) ≤ S(opponents) for every opponent

### Edge cases
- Closed pile exhaustion: replenish closed pile by reshuffling all cards except those in players’ hands and the current open pile
- Ties: Declaration with equality is valid (≤); if multiple players tie with the declarer, it is still a valid declaration
- Reconnects (implementation detail): server should provide full state snapshots so returning clients can resync hands, open pile, closed size, scores, current player, and timers

### Defaults summary
- Cards per player: 5 (suggested)
- Exit score: 100 (default)
- Invalid declaration penalty: 40 (suggested)
- Move timer: 30s (current code default)


