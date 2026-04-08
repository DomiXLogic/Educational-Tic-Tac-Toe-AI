package com.ai.tictactoe.controller;

import com.ai.tictactoe.ai.ComputerStrategy;
import com.ai.tictactoe.ai.MctsStrategy;
import com.ai.tictactoe.ai.MinimaxStrategy;
import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.GameMode;
import com.ai.tictactoe.model.Player;
import com.ai.tictactoe.model.ScoreBoard;
import com.ai.tictactoe.model.StartMode;

public final class GameController {

    public static final int MIN_MCTS_SIMULATIONS = 100;
    public static final int MAX_MCTS_SIMULATIONS = 10_000;
    public static final int DEFAULT_MCTS_SIMULATIONS = 2_000;

    private final GameView view;
    private final AiMoveExecutor aiMoveExecutor;
    private final Player humanPlayer;
    private final Player computerPlayer;
    private final MinimaxStrategy minimaxStrategy;
    private final MctsStrategy mctsStrategy;

    private Board board;
    private GameMode gameMode;
    private StartMode startMode;
    private ScoreBoard scoreBoard;
    private ComputerStrategy activeStrategy;
    private boolean aiThinking;
    private boolean gameOver;
    private long aiTaskToken;
    private Player alternatingStarter;

    public GameController(GameView view, AiMoveExecutor aiMoveExecutor) {
        this.view = view;
        this.aiMoveExecutor = aiMoveExecutor;
        this.humanPlayer = Player.X;
        this.computerPlayer = Player.O;
        this.minimaxStrategy = new MinimaxStrategy();
        this.mctsStrategy = new MctsStrategy(DEFAULT_MCTS_SIMULATIONS);
        this.scoreBoard = ScoreBoard.empty();
        this.gameMode = GameMode.HUMAN_VS_AI_MCTS;
        this.startMode = StartMode.ALWAYS_HUMAN;
        this.activeStrategy = mctsStrategy;
        this.alternatingStarter = humanPlayer;
        startNewGame();
    }

    public void startNewGame() {
        Player startingPlayer = resolveStartingPlayer();
        board = new Board(startingPlayer);
        aiThinking = false;
        gameOver = false;
        aiTaskToken++;
        publishState();
        if (startingPlayer == computerPlayer) {
            triggerComputerMove();
        }
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
        activeStrategy = mode == GameMode.HUMAN_VS_AI_MCTS ? mctsStrategy : minimaxStrategy;
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

    public void handleHumanMove(int index) {
        if (aiThinking || gameOver || board.getCurrentPlayer() != humanPlayer || !board.isMoveValid(index)) {
            return;
        }

        board = board.makeMove(index);
        if (finishGameIfTerminal()) {
            return;
        }

        triggerComputerMove();
    }

    private void triggerComputerMove() {
        aiThinking = true;
        long taskToken = ++aiTaskToken;
        Board taskBoard = board.copy();
        ComputerStrategy strategy = activeStrategy;
        publishState();

        aiMoveExecutor.execute(
                taskBoard,
                computerPlayer,
                strategy,
                move -> applyComputerMove(taskToken, move),
                error -> handleAiFailure(taskToken, error));
    }

    private void applyComputerMove(long taskToken, int move) {
        if (taskToken != aiTaskToken || !aiThinking || gameOver) {
            return;
        }

        aiThinking = false;
        if (board.isMoveValid(move)) {
            board = board.makeMove(move);
        }

        if (!finishGameIfTerminal()) {
            publishState();
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
        Player winner = board.getWinner();
        if (winner == humanPlayer) {
            scoreBoard = scoreBoard.withHumanWin();
        } else if (winner == computerPlayer) {
            scoreBoard = scoreBoard.withComputerWin();
        } else {
            scoreBoard = scoreBoard.withDraw();
        }
        publishState();
        return true;
    }

    private void publishState() {
        publishState(buildStatusMessage());
    }

    private void publishState(String statusMessage) {
        view.render(new GameSnapshot(
                board.copy(),
                gameMode,
                startMode,
                mctsStrategy.getSimulationCount(),
                scoreBoard,
                !aiThinking && !gameOver && board.getCurrentPlayer() == humanPlayer,
                aiThinking,
                gameOver,
                statusMessage));
    }

    private String buildStatusMessage() {
        String modeText = "Mode: " + gameMode;
        if (gameOver) {
            Player winner = board.getWinner();
            if (winner == humanPlayer) {
                return modeText + " | Winner: Human";
            }
            if (winner == computerPlayer) {
                return modeText + " | Winner: Computer";
            }
            return modeText + " | Result: Draw";
        }

        if (aiThinking) {
            return modeText + " | AI is thinking...";
        }

        String turn = board.getCurrentPlayer() == humanPlayer ? "Human (X)" : "Computer (O)";
        return modeText + " | Turn: " + turn;
    }

    private Player resolveStartingPlayer() {
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
