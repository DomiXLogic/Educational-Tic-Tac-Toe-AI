# Project Structure

## Objective
Desktop Tic-Tac-Toe application in Java Swing with two computer opponents:

- `Human vs AI (MCTS)` with adjustable simulation budget
- `Human vs Algo (Minimax)` with perfect play

The architecture is organized so UI, game rules, and AI algorithms stay independent enough to evolve separately.

## High-Level Design

### Layers
- `model`: immutable game state and domain enums
- `ai`: strategy abstraction plus `Minimax` and `MCTS` implementations
- `controller`: game flow orchestration, mode switching, scoreboard, AI triggering
- `ui`: Swing widgets, window composition, and background AI execution integration

### Main runtime flow
1. `AiTiTacToe` starts Swing on the EDT and opens `MainFrame`.
2. `MainFrame` creates `GameController` and provides:
   - a `GameView` implementation for rendering snapshots
   - a Swing-specific `AiMoveExecutor` for background AI work
3. `GameController` owns the active `Board`, selected `GameMode`, scoreboard, and strategy instances.
4. Human input is sent from `BoardPanel` to `GameController`.
5. Controller updates the immutable `Board`, publishes a new `GameSnapshot`, and triggers the AI when needed.
6. AI move calculation runs off the EDT through `SwingWorker`, then returns safely to the controller/UI.

## Domain Model

### `Board`
Immutable value object optimized for repeated copying.

- Internal state:
  - `char[9] cells`
  - `Player currentPlayer`
- Responsibilities:
  - validate moves
  - apply moves by returning a new board
  - list legal moves
  - detect winner, draw, terminal state
  - expose winning line for UI highlighting

Reasoning:
- immutability avoids shared-state bugs between UI, Minimax, and MCTS
- compact array storage keeps simulations cheap

### `Player`
Enum for `X`, `O`, and `EMPTY`.

### `GameMode`
Enum describing available game modes and whether the MCTS slider is relevant.

### `StartMode`
Enum describing who starts each new game:
- always human
- always computer
- alternating one by one

### `ScoreBoard`
Immutable record storing:
- human wins
- computer wins
- draws

## AI Layer

### `ComputerStrategy`
Common contract:

```java
int chooseMove(Board board, Player aiPlayer);
```

This keeps the controller independent from concrete AI implementations.

### `MinimaxStrategy`
Perfect adversarial search for Tic-Tac-Toe.

- recursive search
- terminal-state evaluation only
- alpha-beta pruning
- depth-aware scoring to prefer faster wins and slower losses

### `MctsStrategy`
Configurable Monte Carlo Tree Search implementation.

Four explicit phases are preserved:
- selection
- expansion
- simulation
- backpropagation

`MctsNode` stores:
- board state
- parent and children
- move that produced the node
- untried moves
- visit count
- accumulated score
- `playerJustMoved`

Scoring policy:
- win for `playerJustMoved`: `1.0`
- draw: `0.5`
- loss: `0.0`

This keeps UCT selection consistent for alternating turns.

Low-budget calibration:
- the internal search is still standard MCTS
- final move selection becomes more stochastic at low simulation counts
- this is intentional because Tic-Tac-Toe is small enough that pure MCTS stays very strong even at `100` simulations
- the slider therefore controls both search budget and how deterministic the final move choice is, so low values remain human-beatable

## Controller Layer

### `GameController`
Single coordinator for game rules and application flow.

Owns:
- current `Board`
- selected `GameMode`
- selected `StartMode`
- active strategy
- MCTS simulation count
- scoreboard
- AI-thinking state

Responsibilities:
- start new game
- reset score
- apply human moves
- switch mode and strategy
- resolve who starts each new game, including alternating starter mode
- trigger computer moves
- detect terminal states
- publish `GameSnapshot` to the UI

### `GameSnapshot`
Read-only data passed from controller to UI with:
- current board
- selected mode
- simulation count
- score
- status text
- board enabled/disabled state
- thinking / game-over flags

### `AiMoveExecutor`
Abstraction for asynchronous AI calculation.

Why it exists:
- controller decides *when* AI should run
- Swing UI decides *how* background execution is performed

Current implementation:
- `ui.SwingAiMoveExecutor` uses `SwingWorker`

## UI Layer

### `MainFrame`
Main window composition:
- mode selector
- start selector
- MCTS slider and value label
- light/dark theme switch
- `BoardPanel`
- status label
- scoreboard
- `New Game`
- `Reset Score`

It renders `GameSnapshot` objects and forwards user actions to the controller.

### Theme management
- `ui.ThemeManager` owns runtime look-and-feel switching
- application starts in `FlatMacLightLaf`
- user can switch to `FlatMacDarkLaf` from the main window without restarting
- theme changes stay in the UI layer and do not affect controller or AI logic

### `BoardPanel`
Dedicated panel for the 3x3 board using Swing buttons.

Responsibilities:
- render symbols
- enable only valid clicks when input is allowed
- highlight winning line
- animate the winning line with a lightweight Swing timer pulse effect
- forward clicked cell index to the controller

## Threading Model

### EDT safety
- Swing component creation and updates happen on the EDT
- AI computation happens off the EDT through `SwingWorker`

### Stale result protection
Controller uses a monotonically increasing AI task token.

If the user starts a new game or changes mode while an older AI computation is still running, the outdated result is ignored instead of mutating the new game state.

## Extensibility Notes
The current design leaves room for:
- extra strategies through `ComputerStrategy`
- player-side selection (`X` or `O`)
- AI vs AI
- move history / analytics
- theme changes isolated to UI classes
- larger board games reusing the controller/strategy split

## Current Implementation Scope
The current implementation provides a complete playable desktop application, including:
- Minimax mode
- MCTS mode with slider
- selectable starting player
- light/dark theme support
- background AI thinking
- score tracking
- win animation feedback
- README and architecture documentation
