package com.ai.tictactoe.controller;

import com.ai.tictactoe.ai.ComputerStrategy;
import com.ai.tictactoe.ai.MctsStrategy;
import com.ai.tictactoe.ai.MinimaxStrategy;
import com.ai.tictactoe.monitor.TelemetrySink;
import com.ai.tictactoe.model.ArtificialStupidityLevel;
import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.BoardSize;
import com.ai.tictactoe.model.BotStrategyType;
import com.ai.tictactoe.model.GameMode;
import com.ai.tictactoe.model.Player;
import com.ai.tictactoe.model.ScoreBoard;
import com.ai.tictactoe.model.StartMode;
import com.ai.tictactoe.telemetry.AiMoveResult;

public final class GameController {

    public static final int MIN_MCTS_SIMULATIONS = 100;
    public static final int MAX_MCTS_SIMULATIONS = 10_000;
    public static final int DEFAULT_MCTS_SIMULATIONS = 2_000;
    public static final int MIN_PC_VS_PC_DELAY_MS = 300;
    public static final int MAX_PC_VS_PC_DELAY_MS = 5_000;
    public static final int DEFAULT_PC_VS_PC_DELAY_MS = 1_200;

    private final GameView view;
    private final AiMoveExecutor aiMoveExecutor;
    private final AutoMoveScheduler autoMoveScheduler;
    private final TelemetrySink telemetrySink;
    private final Player humanPlayer;
    private final Player computerPlayer;
    private final MinimaxStrategy minimaxStrategy;
    private final MctsStrategy mctsStrategy;
    private final MctsStrategy xMctsStrategy;
    private final MctsStrategy oMctsStrategy;

    private Board board;
    private BoardSize boardSize;
    private GameMode gameMode;
    private StartMode startMode;
    private ScoreBoard scoreBoard;
    private boolean aiThinking;
    private boolean gameOver;
    private long aiTaskToken;
    private long currentGameId;
    private int lastComputerMove;
    private Player alternatingStarter;
    private BotStrategyType xBotStrategy;
    private BotStrategyType oBotStrategy;
    private boolean mctsArtificialStupidityEnabled;
    private ArtificialStupidityLevel mctsStupidityLevel;
    private boolean xMctsArtificialStupidityEnabled;
    private boolean oMctsArtificialStupidityEnabled;
    private ArtificialStupidityLevel xMctsStupidityLevel;
    private ArtificialStupidityLevel oMctsStupidityLevel;
    private int pcVsPcMoveDelayMillis;
    private boolean pcVsPcRunning;

    public GameController(GameView view,
                          AiMoveExecutor aiMoveExecutor,
                          AutoMoveScheduler autoMoveScheduler,
                          TelemetrySink telemetrySink) {
        this.view = view;
        this.aiMoveExecutor = aiMoveExecutor;
        this.autoMoveScheduler = autoMoveScheduler;
        this.telemetrySink = telemetrySink;
        this.humanPlayer = Player.X;
        this.computerPlayer = Player.O;
        this.minimaxStrategy = new MinimaxStrategy();
        this.mctsStrategy = new MctsStrategy(DEFAULT_MCTS_SIMULATIONS);
        this.xMctsStrategy = new MctsStrategy(DEFAULT_MCTS_SIMULATIONS);
        this.oMctsStrategy = new MctsStrategy(DEFAULT_MCTS_SIMULATIONS);
        this.scoreBoard = ScoreBoard.empty();
        this.boardSize = BoardSize.THREE;
        this.gameMode = GameMode.HUMAN_VS_AI_MCTS;
        this.startMode = StartMode.ALWAYS_HUMAN;
        this.lastComputerMove = -1;
        this.alternatingStarter = humanPlayer;
        this.xBotStrategy = BotStrategyType.ALGO;
        this.oBotStrategy = BotStrategyType.MCTS;
        this.mctsArtificialStupidityEnabled = true;
        this.mctsStupidityLevel = ArtificialStupidityLevel.EXTRA_HIGH;
        this.xMctsArtificialStupidityEnabled = true;
        this.oMctsArtificialStupidityEnabled = true;
        this.xMctsStupidityLevel = ArtificialStupidityLevel.EXTRA_HIGH;
        this.oMctsStupidityLevel = ArtificialStupidityLevel.EXTRA_HIGH;
        this.pcVsPcMoveDelayMillis = DEFAULT_PC_VS_PC_DELAY_MS;
        this.pcVsPcRunning = false;
        this.mctsStrategy.setArtificialStupidityEnabled(mctsArtificialStupidityEnabled);
        this.mctsStrategy.setArtificialStupidityLevel(mctsStupidityLevel);
        this.xMctsStrategy.setArtificialStupidityEnabled(xMctsArtificialStupidityEnabled);
        this.xMctsStrategy.setArtificialStupidityLevel(xMctsStupidityLevel);
        this.oMctsStrategy.setArtificialStupidityEnabled(oMctsArtificialStupidityEnabled);
        this.oMctsStrategy.setArtificialStupidityLevel(oMctsStupidityLevel);
        startNewGame();
    }

    public void startNewGame() {
        autoMoveScheduler.cancel();
        Player startingPlayer = resolveStartingPlayer();
        currentGameId++;
        board = new Board(startingPlayer, boardSize);
        aiThinking = false;
        gameOver = false;
        lastComputerMove = -1;
        if (gameMode.isPcVsPc()) {
            pcVsPcRunning = false;
        }
        aiTaskToken++;
        publishState();
        triggerAutomatedMoveIfNeeded();
    }

    public void resetScore() {
        scoreBoard = ScoreBoard.empty();
        publishState();
    }

    public void changeMode(GameMode mode) {
        if (mode == null || mode == gameMode) {
            return;
        }
        gameMode = mode;
        startNewGame();
    }

    public void changeBoardSize(BoardSize size) {
        if (size == null || size == boardSize) {
            return;
        }
        boardSize = size;
        scoreBoard = ScoreBoard.empty();
        mctsStrategy.setSimulationCount(size.getRecommendedSimulationCount());
        xMctsStrategy.setSimulationCount(size.getRecommendedSimulationCount());
        oMctsStrategy.setSimulationCount(size.getRecommendedSimulationCount());
        startNewGame();
    }

    public void changeStartMode(StartMode mode) {
        if (mode == null || mode == startMode) {
            return;
        }
        startMode = mode;
        if (mode == StartMode.ONE_BY_ONE) {
            alternatingStarter = humanPlayer;
        }
        startNewGame();
    }

    public void changeMctsSimulationCount(int simulationCount) {
        int clamped = Math.max(MIN_MCTS_SIMULATIONS, Math.min(MAX_MCTS_SIMULATIONS, simulationCount));
        mctsStrategy.setSimulationCount(clamped);
        publishState();
    }

    public void changeMctsStupidityLevel(ArtificialStupidityLevel level) {
        if (level == null || level == mctsStupidityLevel) {
            return;
        }
        mctsStupidityLevel = level;
        mctsStrategy.setArtificialStupidityLevel(level);
        publishState();
    }

    public void changeMctsArtificialStupidityEnabled(boolean enabled) {
        if (mctsArtificialStupidityEnabled == enabled) {
            return;
        }
        mctsArtificialStupidityEnabled = enabled;
        mctsStrategy.setArtificialStupidityEnabled(enabled);
        publishState();
    }

    public void changePcVsPcStrategy(Player player, BotStrategyType strategyType) {
        if (player == null || strategyType == null) {
            return;
        }
        if (player == Player.X) {
            if (xBotStrategy == strategyType) {
                return;
            }
            xBotStrategy = strategyType;
        } else if (player == Player.O) {
            if (oBotStrategy == strategyType) {
                return;
            }
            oBotStrategy = strategyType;
        } else {
            return;
        }
        startNewGame();
    }

    public void changePcVsPcSimulationCount(Player player, int simulationCount) {
        int clamped = Math.max(MIN_MCTS_SIMULATIONS, Math.min(MAX_MCTS_SIMULATIONS, simulationCount));
        if (player == Player.X) {
            xMctsStrategy.setSimulationCount(clamped);
        } else if (player == Player.O) {
            oMctsStrategy.setSimulationCount(clamped);
        }
        publishState();
    }

    public void changePcVsPcStupidityLevel(Player player, ArtificialStupidityLevel level) {
        if (player == null || level == null) {
            return;
        }
        if (player == Player.X) {
            if (xMctsStupidityLevel == level) {
                return;
            }
            xMctsStupidityLevel = level;
            xMctsStrategy.setArtificialStupidityLevel(level);
        } else if (player == Player.O) {
            if (oMctsStupidityLevel == level) {
                return;
            }
            oMctsStupidityLevel = level;
            oMctsStrategy.setArtificialStupidityLevel(level);
        } else {
            return;
        }
        publishState();
    }

    public void changePcVsPcArtificialStupidityEnabled(Player player, boolean enabled) {
        if (player == null) {
            return;
        }
        if (player == Player.X) {
            if (xMctsArtificialStupidityEnabled == enabled) {
                return;
            }
            xMctsArtificialStupidityEnabled = enabled;
            xMctsStrategy.setArtificialStupidityEnabled(enabled);
        } else if (player == Player.O) {
            if (oMctsArtificialStupidityEnabled == enabled) {
                return;
            }
            oMctsArtificialStupidityEnabled = enabled;
            oMctsStrategy.setArtificialStupidityEnabled(enabled);
        } else {
            return;
        }
        publishState();
    }

    public void changePcVsPcMoveDelay(int delayMillis) {
        int clamped = Math.max(MIN_PC_VS_PC_DELAY_MS, Math.min(MAX_PC_VS_PC_DELAY_MS, delayMillis));
        pcVsPcMoveDelayMillis = clamped;
        publishState();
    }

    public void startPcVsPc() {
        if (!gameMode.isPcVsPc() || gameOver) {
            return;
        }
        pcVsPcRunning = true;
        publishState();
        triggerAutomatedMoveIfNeeded();
    }

    public void pausePcVsPc() {
        if (!gameMode.isPcVsPc()) {
            return;
        }
        pcVsPcRunning = false;
        autoMoveScheduler.cancel();
        publishState();
    }

    public void handleHumanMove(int index) {
        if (!isHumanTurn() || aiThinking || gameOver || !board.isMoveValid(index)) {
            return;
        }

        board = board.makeMove(index);
        if (finishGameIfTerminal()) {
            return;
        }

        triggerAutomatedMoveIfNeeded();
    }

    private boolean isHumanTurn() {
        return !gameMode.isPcVsPc() && board.getCurrentPlayer() == humanPlayer;
    }

    private boolean isAutomatedTurn() {
        if (gameOver || aiThinking) {
            return false;
        }
        if (gameMode.isPcVsPc()) {
            if (!pcVsPcRunning) {
                return false;
            }
            return true;
        }
        return board.getCurrentPlayer() == computerPlayer;
    }

    private void triggerAutomatedMoveIfNeeded() {
        if (!isAutomatedTurn()) {
            return;
        }

        if (gameMode.isPcVsPc()) {
            long scheduleToken = aiTaskToken;
            autoMoveScheduler.schedule(pcVsPcMoveDelayMillis, () -> {
                if (scheduleToken != aiTaskToken || gameOver || aiThinking) {
                    return;
                }
                triggerComputerMove(board.getCurrentPlayer(), resolveStrategy(board.getCurrentPlayer()));
            });
            return;
        }

        triggerComputerMove(computerPlayer, resolveStrategy(computerPlayer));
    }

    private void triggerComputerMove(Player automatedPlayer, ComputerStrategy strategy) {
        if (strategy == null) {
            return;
        }

        aiThinking = true;
        autoMoveScheduler.cancel();
        long taskToken = ++aiTaskToken;
        Board taskBoard = board.copy();
        publishState();

        aiMoveExecutor.execute(
                taskBoard,
                automatedPlayer,
                strategy,
                result -> applyComputerMove(taskToken, automatedPlayer, result),
                error -> handleAiFailure(taskToken, error));
    }

    private ComputerStrategy resolveStrategy(Player player) {
        return switch (gameMode) {
            case HUMAN_VS_AI_MCTS -> mctsStrategy;
            case HUMAN_VS_ALGO_MINIMAX -> minimaxStrategy;
            case PC_VS_PC -> resolvePcVsPcStrategy(player);
        };
    }

    private ComputerStrategy resolvePcVsPcStrategy(Player player) {
        BotStrategyType strategyType = player == Player.X ? xBotStrategy : oBotStrategy;
        if (strategyType == BotStrategyType.MCTS) {
            return player == Player.X ? xMctsStrategy : oMctsStrategy;
        }
        return minimaxStrategy;
    }

    private void applyComputerMove(long taskToken, Player automatedPlayer, AiMoveResult result) {
        if (taskToken != aiTaskToken || !aiThinking || gameOver || board.getCurrentPlayer() != automatedPlayer) {
            return;
        }

        aiThinking = false;
        int move = result.move();
        if (board.isMoveValid(move)) {
            board = board.makeMove(move);
            lastComputerMove = move;
            if (telemetrySink != null) {
                telemetrySink.recordAiMove(currentGameId, result.telemetry());
            }
        }

        if (!finishGameIfTerminal()) {
            publishState();
            triggerAutomatedMoveIfNeeded();
        }
    }

    private void handleAiFailure(long taskToken, Throwable error) {
        if (taskToken != aiTaskToken) {
            return;
        }
        aiThinking = false;
        publishState("AI move failed: " + error.getMessage());
    }

    private boolean finishGameIfTerminal() {
        if (!board.isTerminal()) {
            return false;
        }

        gameOver = true;
        aiThinking = false;
        autoMoveScheduler.cancel();
        pcVsPcRunning = false;
        Player winner = board.getWinner();
        if (winner == humanPlayer) {
            scoreBoard = scoreBoard.withHumanWin();
            if (telemetrySink != null) {
                telemetrySink.recordGameOutcome(currentGameId, gameMode.isPcVsPc() ? "X Win" : "Human Win");
            }
        } else if (winner == computerPlayer) {
            scoreBoard = scoreBoard.withComputerWin();
            if (telemetrySink != null) {
                telemetrySink.recordGameOutcome(currentGameId, gameMode.isPcVsPc() ? "O Win" : "Computer Win");
            }
        } else {
            scoreBoard = scoreBoard.withDraw();
            if (telemetrySink != null) {
                telemetrySink.recordGameOutcome(currentGameId, "Draw");
            }
        }
        publishState();
        return true;
    }

    private void publishState() {
        publishState(buildStatusMessage());
    }

    private void publishState(String statusMessage) {
        view.render(new GameSnapshot(
                currentGameId,
                board.copy(),
                boardSize,
                gameMode,
                startMode,
                mctsStrategy.getSimulationCount(),
                mctsArtificialStupidityEnabled,
                mctsStupidityLevel,
                xBotStrategy,
                oBotStrategy,
                xMctsStrategy.getSimulationCount(),
                oMctsStrategy.getSimulationCount(),
                xMctsArtificialStupidityEnabled,
                oMctsArtificialStupidityEnabled,
                xMctsStupidityLevel,
                oMctsStupidityLevel,
                pcVsPcMoveDelayMillis,
                pcVsPcRunning,
                lastComputerMove,
                scoreBoard,
                isHumanTurn(),
                aiThinking,
                gameOver,
                statusMessage));
    }

    private String buildStatusMessage() {
        String modeText = "Mode: " + gameMode
                + " | Board: " + boardSize.getBoardDimension() + "x" + boardSize.getBoardDimension()
                + " | Goal: " + boardSize.getWinLength() + " in a row";
        if (gameMode == GameMode.HUMAN_VS_ALGO_MINIMAX) {
            modeText += boardSize.usesExactAlgo()
                    ? " | Algo: Exact search"
                    : " | Algo: Heuristic search";
        } else if (gameMode == GameMode.HUMAN_VS_AI_MCTS) {
            modeText += " | MCTS: " + mctsStrategy.getSimulationCount()
                    + " | AS: " + (mctsArtificialStupidityEnabled
                    ? mctsStupidityLevel.getDisplayName()
                    : "Off");
        } else if (gameMode.isPcVsPc()) {
            modeText += " | X: " + resolvePcVsPcStrategyLabel(Player.X)
                    + " | O: " + resolvePcVsPcStrategyLabel(Player.O)
                    + " | Speed: " + formatMoveDelay();
        }
        if (gameOver) {
            Player winner = board.getWinner();
            if (winner == humanPlayer) {
                return modeText + " | Winner: " + (gameMode.isPcVsPc() ? "X" : "Human");
            }
            if (winner == computerPlayer) {
                return modeText + " | Winner: " + (gameMode.isPcVsPc() ? "O" : "Computer");
            }
            return modeText + " | Result: Draw";
        }

        if (aiThinking) {
            return modeText + " | AI is thinking...";
        }

        if (gameMode.isPcVsPc()) {
            if (!pcVsPcRunning) {
                return modeText + " | Ready";
            }
            return modeText + " | Turn: " + board.getCurrentPlayer() + " (" + resolvePcVsPcStrategyLabel(board.getCurrentPlayer()) + ")";
        }

        String turn = board.getCurrentPlayer() == humanPlayer ? "Human (X)" : "Computer (O)";
        return modeText + " | Turn: " + turn;
    }

    private String resolvePcVsPcStrategyLabel(Player player) {
        BotStrategyType type = player == Player.X ? xBotStrategy : oBotStrategy;
        if (type == BotStrategyType.MCTS) {
            int simulations = player == Player.X ? xMctsStrategy.getSimulationCount() : oMctsStrategy.getSimulationCount();
            ArtificialStupidityLevel level = player == Player.X ? xMctsStupidityLevel : oMctsStupidityLevel;
            boolean enabled = player == Player.X ? xMctsArtificialStupidityEnabled : oMctsArtificialStupidityEnabled;
            return "MCTS " + simulations + " / " + (enabled ? level.getDisplayName() : "AS Off");
        }
        return "Algo";
    }

    private String formatMoveDelay() {
        return String.format("%.1f sec/move", pcVsPcMoveDelayMillis / 1000.0);
    }

    private Player resolveStartingPlayer() {
        if (gameMode.isPcVsPc()) {
            return Player.X;
        }

        return switch (startMode) {
            case ALWAYS_HUMAN -> humanPlayer;
            case ALWAYS_PC -> computerPlayer;
            case ONE_BY_ONE -> resolveAlternatingStarter();
        };
    }

    private Player resolveAlternatingStarter() {
        Player starter = alternatingStarter;
        alternatingStarter = alternatingStarter == humanPlayer ? computerPlayer : humanPlayer;
        return starter;
    }
}
