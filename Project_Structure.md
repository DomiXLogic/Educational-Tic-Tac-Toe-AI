# Project Structure

## Objective
Desktop Tic-Tac-Toe application in Java Swing with two computer opponents:

- `Human vs AI (MCTS)` with adjustable simulation budget and a separate Artificial Stupidity control
- `Human vs Algo` with exact search on `3x3` and heuristic search on larger boards
- `PC vs PC` with independent side configuration and autoplay speed control
- scalable board sizes from `3x3` to `6x6`

The architecture is organized so UI, game rules, and AI algorithms stay independent enough to evolve separately.

## High-Level Design

### Layers
- `model`: immutable game state and domain enums
- `ai`: strategy abstraction plus `Minimax` and `MCTS` implementations
- `controller`: game flow orchestration, mode switching, scoreboard, AI triggering, and autoplay scheduling
- `ui`: Swing widgets, window composition, PC-vs-PC controls, and background AI execution integration
- `monitor`: research-oriented telemetry window, charts, history tables, and summaries

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
  - `char[] cells`
  - `Player currentPlayer`
  - `BoardSize boardSize`
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

### `BoardSize`
Enum describing the supported game levels from `3x3` to `6x6`.

Ruleset:
- `3x3` uses a classic `3x3` board with `3 in a row` to win
- levels `4x4` through `6x6` use a `10x10` board
- the selected level defines the required win length
- this keeps larger games more open and reduces trivial draw states

Each size also carries configuration for:
- actual board dimension
- win length
- recommended MCTS simulation count
- whether exact algorithmic search is still practical
- heuristic search depth for larger boards

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
AiMoveResult chooseMove(Board board, Player aiPlayer);
```

This keeps the controller independent from concrete AI implementations while also allowing each strategy to return telemetry together with the chosen move.

### `MinimaxStrategy`
Algorithmic opponent strategy.

Behavior depends on board size:
- `3x3`: exact Minimax with alpha-beta pruning
- `4x4` to `6x6`: heuristic depth-limited alpha-beta search

Larger boards intentionally switch away from perfect search because the full game tree becomes impractical. This lets the project demonstrate the limits of exact search as board size grows.

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

Controlled imperfection:
- the internal search is still standard MCTS
- the simulation budget controls how much search work is done
- an Artificial Stupidity enable/disable flag decides whether the teaching-oriented softening is active at all
- when Artificial Stupidity is disabled, MCTS runs in its strongest project configuration and keeps tactical guardrails enabled
- when Artificial Stupidity is enabled, a separate level controls how strongly the final move selection is softened
- immediate tactical wins are always taken
- immediate losing replies are filtered out when a safe move already exists in the strong no-AS mode
- open-ended tactical threats are also handled more aggressively in the strong no-AS mode
- expansion and rollout use local tactical/positional scoring so large-board MCTS is less aimless
- this is intentional because Tic-Tac-Toe is small enough that pure MCTS can otherwise look either too perfect or too arbitrarily weak, depending on the board size and search budget

## Controller Layer

### `GameController`
Single coordinator for game rules and application flow.

Owns:
- current `Board`
- selected `BoardSize`
- selected `GameMode`
- selected `StartMode`
- MCTS simulation count
- Human-vs-AI MCTS Artificial Stupidity enabled flag
- Human-vs-AI MCTS Artificial Stupidity level
- side-specific bot strategies for `PC vs PC`
- side-specific MCTS budgets for `PC vs PC`
- side-specific MCTS Artificial Stupidity enabled flags for `PC vs PC`
- side-specific MCTS Artificial Stupidity levels for `PC vs PC`
- autoplay speed for `PC vs PC`
- scoreboard
- AI-thinking state

Responsibilities:
- start new game
- reset score
- change board size and apply size-specific defaults
- keep the selected level synchronized with the actual board dimension and win condition
- apply human moves
- switch mode and strategy
- resolve who starts each new game, including alternating starter mode
- trigger computer moves
- coordinate autoplay scheduling for `PC vs PC`
- detect terminal states
- publish AI telemetry and final outcomes to the monitor window
- publish `GameSnapshot` to the UI

### `GameSnapshot`
Read-only data passed from controller to UI with:
- current game id
- current board
- selected mode
- simulation count
- PC-vs-PC side strategies
- PC-vs-PC side simulation counts
- PC-vs-PC move delay
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

### `AutoMoveScheduler`
Abstraction for delayed autoplay moves.

Why it exists:
- controller decides when the next automated move should happen
- Swing UI decides how timed scheduling is executed on the EDT

Current implementation:
- `ui.SwingAutoMoveScheduler` uses a one-shot Swing `Timer`

## UI Layer

### `MainFrame`
Main window composition:
- mode selector
- level selector
- win-condition explanation label
- start selector
- MCTS simulation slider and value label
- Artificial Stupidity checkbox
- Artificial Stupidity slider and value label
- PC-vs-PC configuration panel with:
  - side-by-side `X Bot` and `O Bot` panels
  - `X` strategy selector
  - `O` strategy selector
  - side-specific MCTS sliders
  - side-specific Artificial Stupidity checkboxes
  - side-specific Artificial Stupidity sliders
  - autoplay speed slider
  - `Start` / `Pause` controls for manual autoplay
- on-demand `Show AI Move` button for flashing the latest computer move
- menu bar with `File`, `View`, and `Help`
- `BoardPanel`
- status label
- scoreboard
- `New Game`
- `Reset Score`

It renders `GameSnapshot` objects and forwards user actions to the controller.

Menu responsibilities:
- `File -> Close`
- `View -> Algorithm Monitor`
- `View -> Dark UI`
- `Help -> About`
- `Help -> Metrics Doc`

### Theme management
- `ui.ThemeManager` owns runtime look-and-feel switching
- application starts in `FlatMacLightLaf`
- user can switch to `FlatMacDarkLaf` from the main window without restarting
- theme changes stay in the UI layer and do not affect controller or AI logic

### `BoardPanel`
Dedicated panel for the dynamic board using Swing buttons.

Responsibilities:
- render symbols
- rebuild the grid dynamically when board size changes
- enable only valid clicks when input is allowed
- highlight winning line
- animate the winning line with a lightweight Swing timer pulse effect
- flash the latest AI move on demand without changing the permanent board state
- forward clicked cell index to the controller

### `AlgorithmMonitorFrame`
Separate research window for observing:

- live process CPU and heap usage
- current move telemetry
- algorithm internals such as nodes, cutoffs, simulations, rollout statistics, and confidence
- top candidate moves
- historical move telemetry
- aggregated summary comparisons

The monitor stays outside the main gameplay loop so the game window remains clean while still exposing detailed analysis when needed.

### Telemetry pipeline
- `ComputerStrategy` now returns an `AiMoveResult`
- strategies produce algorithm-specific telemetry together with the chosen move
- `SwingAiMoveExecutor` wraps that result with process-level metrics such as CPU time, heap, threads, and GC counters
- `GameController` forwards telemetry to the monitor window and associates it with the current game id

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
- board sizes from `3x3` to `6x6`
- `10x10` board for levels above `3x3`
- exact algorithmic mode on `3x3`
- heuristic algorithmic mode on larger boards
- MCTS mode with simulation and Artificial Stupidity sliders
- PC-vs-PC autoplay with per-side strategy selection
- Algorithm Monitor with live charts and research tables
- selectable starting player
- light/dark theme support
- background AI thinking
- score tracking
- win animation feedback
- README and architecture documentation
