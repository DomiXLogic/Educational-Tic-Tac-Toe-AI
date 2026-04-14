package com.ai.tictactoe.ai;

import com.ai.tictactoe.model.ArtificialStupidityLevel;
import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.Player;
import com.ai.tictactoe.telemetry.AiMoveResult;
import com.ai.tictactoe.telemetry.AiMoveTelemetry;
import com.ai.tictactoe.telemetry.MoveCandidate;
import java.util.List;
import java.util.Random;

/**
 * Monte Carlo Tree Search strategy used for the educational "Human vs AI"
 * mode.
 *
 * <p>This implementation keeps the standard four MCTS phases:
 * selection, expansion, simulation, and backpropagation.
 *
 * <p>It also applies a calibrated final move policy at low simulation counts.
 * That extra step is intentional and is part of the project's educational
 * "Artificial Stupidity" concept: the search remains meaningful, but the
 * visible behavior becomes easier to compare against a perfect Minimax
 * opponent.
 */
public final class MctsStrategy implements ComputerStrategy {

    private static final double EXPLORATION_CONSTANT = Math.sqrt(2.0);
    private static final int BASELINE_SIMULATIONS = 100;
    private static final int MAX_TUNED_SIMULATIONS = 10_000;
    private static final int[][] DIRECTIONS = {
        {0, 1},
        {1, 0},
        {1, 1},
        {1, -1}
    };
    private static final int EXPANSION_SAMPLE_LIMIT = 12;
    private static final int ROLLOUT_CANDIDATE_LIMIT = 14;
    private static final long MAX_TIME_BUDGET_MS = 4_500L;

    private final Random random;
    private int simulationCount;
    private boolean artificialStupidityEnabled;
    private ArtificialStupidityLevel artificialStupidityLevel;

    public MctsStrategy(int simulationCount) {
        this(simulationCount, new Random());
    }

    MctsStrategy(int simulationCount, Random random) {
        this.simulationCount = simulationCount;
        this.random = random;
        this.artificialStupidityEnabled = true;
        this.artificialStupidityLevel = ArtificialStupidityLevel.EXTRA_HIGH;
    }

    /**
     * Updates the simulation budget used by MCTS.
     *
     * <p>In this project the budget also indirectly affects how deterministic
     * the final move selection becomes.
     */
    public void setSimulationCount(int simulationCount) {
        this.simulationCount = simulationCount;
    }

    public int getSimulationCount() {
        return simulationCount;
    }

    public void setArtificialStupidityLevel(ArtificialStupidityLevel artificialStupidityLevel) {
        this.artificialStupidityLevel = artificialStupidityLevel == null
                ? ArtificialStupidityLevel.EXTRA_HIGH
                : artificialStupidityLevel;
    }

    public void setArtificialStupidityEnabled(boolean artificialStupidityEnabled) {
        this.artificialStupidityEnabled = artificialStupidityEnabled;
    }

    public ArtificialStupidityLevel getArtificialStupidityLevel() {
        return artificialStupidityLevel;
    }

    /**
     * Runs an MCTS search from the current board and returns the move chosen
     * for the AI player.
     */
    @Override
    public AiMoveResult chooseMove(Board board, Player aiPlayer) {
        if (board.getCurrentPlayer() != aiPlayer) {
            throw new IllegalArgumentException("MCTS called when it is not " + aiPlayer + "'s turn.");
        }

        long startedAtNanos = System.nanoTime();
        List<Integer> availableMoves = board.getAvailableMoves();
        if (availableMoves.isEmpty()) {
            throw new IllegalStateException("No legal move available for MCTS.");
        }

        Integer tacticalMove = findGuardrailMove(board, aiPlayer);
        if (tacticalMove != null) {
            return new AiMoveResult(tacticalMove, createSingleMoveTelemetry(board, tacticalMove, startedAtNanos));
        }

        if (availableMoves.size() == 1) {
            int move = availableMoves.get(0);
            return new AiMoveResult(move, createSingleMoveTelemetry(board, move, startedAtNanos));
        }

        MctsNode root = new MctsNode(board.copy(), null, -1, board.getCurrentPlayer().opponent());
        SearchStats stats = new SearchStats();
        long deadlineNanos = System.nanoTime() + calculateTimeBudgetNanos(board);

        for (int simulation = 0; simulation < simulationCount; simulation++) {
            if (System.nanoTime() >= deadlineNanos) {
                break;
            }
            MctsNode node = root;

            // 1. Selection: follow the most promising UCT path while the node
            // is fully expanded and the game is not over.
            while (!node.getBoard().isTerminal() && node.isFullyExpanded() && !node.getChildren().isEmpty()) {
                node = node.selectChildByUct(EXPLORATION_CONSTANT);
                stats.selectionSteps++;
            }

            // 2. Expansion: grow the tree by trying one unexpanded move.
            if (!node.getBoard().isTerminal() && node.hasUntriedMoves()) {
                node = expandPromisingChild(node);
                stats.expandedNodes++;
            }

            // 3. Simulation / rollout: play random moves until the board ends.
            RolloutResult rolloutResult = rollout(node.getBoard());
            stats.totalRolloutMoves += rolloutResult.rolloutLength();
            stats.totalRolloutTimeNanos += rolloutResult.durationNanos();

            // 4. Backpropagation: send the result back through the visited path.
            backpropagate(node, rolloutResult.winner());
            stats.completedSimulations++;
        }

        List<MctsNode> rankedChildren = rankChildren(root);
        MctsNode selectedChild = selectFinalChild(root, rankedChildren);
        long responseTimeMillis = Math.max(1L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
        long completedSimulations = Math.max(1L, stats.completedSimulations);
        AiMoveTelemetry telemetry = new AiMoveTelemetry(
                buildTelemetryName(),
                false,
                selectedChild.getMove(),
                selectedChild.getMove() / board.getSize(),
                selectedChild.getMove() % board.getSize(),
                System.currentTimeMillis(),
                responseTimeMillis,
                board.getSize(),
                board.getWinLength(),
                availableMoves.size(),
                board.getCellCount() - availableMoves.size(),
                detectGamePhase(board),
                stats.expandedNodes + stats.selectionSteps,
                0L,
                0,
                selectedChild.getAverageScore(),
                completedSimulations,
                completedSimulations / Math.max(0.001, responseTimeMillis / 1_000.0),
                (double) stats.totalRolloutMoves / completedSimulations,
                (stats.totalRolloutTimeNanos / 1_000_000.0) / completedSimulations,
                root.getVisitCount() == 0 ? 0.0 : (double) selectedChild.getVisitCount() / root.getVisitCount(),
                mapCandidates(board, rankedChildren),
                null,
                null);
        return new AiMoveResult(selectedChild.getMove(), telemetry);
    }

    /**
     * Plays a simulated game from the provided board until a terminal result is
     * reached.
     */
    private RolloutResult rollout(Board board) {
        long startedAtNanos = System.nanoTime();
        Board rolloutBoard = board.copy();
        int rolloutLength = 0;
        int rolloutDepthLimit = calculateRolloutDepthLimit(rolloutBoard);
        while (!rolloutBoard.isTerminal() && rolloutLength < rolloutDepthLimit) {
            rolloutBoard = rolloutBoard.makeMove(chooseRolloutMove(rolloutBoard));
            rolloutLength++;
        }
        Player winner = rolloutBoard.isTerminal()
                ? rolloutBoard.getWinner()
                : estimateRolloutWinner(rolloutBoard);
        return new RolloutResult(winner, rolloutLength, System.nanoTime() - startedAtNanos);
    }

    /**
     * Updates visits and rewards from the simulated node back to the root.
     */
    private void backpropagate(MctsNode node, Player winner) {
        MctsNode current = node;
        while (current != null) {
            current.updateStats(winner);
            current = current.getParent();
        }
    }

    private List<MctsNode> rankChildren(MctsNode root) {
        return root.getChildren().stream()
                .sorted((left, right) -> {
                    int visitCompare = Integer.compare(right.getVisitCount(), left.getVisitCount());
                    if (visitCompare != 0) {
                        return visitCompare;
                    }
                    return Double.compare(right.getAverageScore(), left.getAverageScore());
                })
                .toList();
    }

    /**
     * Chooses the move shown to the player after search is complete.
     *
     * <p>At high simulation counts this behaves close to standard MCTS. At low
     * counts it intentionally adds controlled stochasticity so the AI is still
     * beatable and the difference from Minimax remains obvious in class.
     */
    private MctsNode selectFinalChild(MctsNode root, List<MctsNode> children) {
        List<MctsNode> tacticallyFilteredChildren = isStrongGuardrailMode()
                ? applyTacticalSafety(root, children)
                : children;
        double imperfection = calculateImperfectionFactor(root.getBoard());
        if (imperfection <= 0.05) {
            // At high strength, fall back to the strongest child directly.
            return tacticallyFilteredChildren.get(0);
        }

        if (random.nextDouble() < imperfection * 0.40) {
            // A small random branch keeps low-strength settings visibly imperfect.
            return tacticallyFilteredChildren.get(random.nextInt(tacticallyFilteredChildren.size()));
        }

        // Most of the time we sample among strong candidates, not blindly.
        double temperature = 0.18 + imperfection * 1.35;
        return sampleSoftmax(tacticallyFilteredChildren, temperature, root.getVisitCount());
    }

    /**
     * Prevents obviously bad final choices such as ignoring an immediate win or
     * allowing an immediate losing reply when a safe move already exists.
     */
    private List<MctsNode> applyTacticalSafety(MctsNode root, List<MctsNode> children) {
        Player currentPlayer = root.getBoard().getCurrentPlayer();

        List<MctsNode> winningChildren = children.stream()
                .filter(child -> child.getBoard().isTerminal() && child.getBoard().getWinner() == currentPlayer)
                .toList();
        if (!winningChildren.isEmpty()) {
            return winningChildren;
        }

        int minimumThreatReplies = Integer.MAX_VALUE;
        List<MctsNode> safestChildren = new java.util.ArrayList<>();
        for (MctsNode child : children) {
            int threatReplies = countImmediateWinningReplies(child.getBoard(), currentPlayer.opponent());
            if (threatReplies < minimumThreatReplies) {
                minimumThreatReplies = threatReplies;
                safestChildren.clear();
                safestChildren.add(child);
            } else if (threatReplies == minimumThreatReplies) {
                safestChildren.add(child);
            }
        }

        return safestChildren.isEmpty() ? children : safestChildren;
    }

    private Integer findGuardrailMove(Board board, Player aiPlayer) {
        List<Integer> moves = board.getAvailableMoves();

        List<Integer> immediateWins = new java.util.ArrayList<>();
        for (int move : moves) {
            if (wouldWinAfterMove(board, move, aiPlayer)) {
                immediateWins.add(move);
            }
        }
        if (!immediateWins.isEmpty()) {
            return immediateWins.get(random.nextInt(immediateWins.size()));
        }

        if (!isStrongGuardrailMode()) {
            return null;
        }

        int currentOpponentThreats = countImmediateWinningReplies(board, aiPlayer.opponent());
        if (currentOpponentThreats > 0) {
            int minimumThreatReplies = Integer.MAX_VALUE;
            List<Integer> safestMoves = new java.util.ArrayList<>();
            for (int move : moves) {
                Board nextBoard = board.makeMove(move);
                int threatReplies = countImmediateWinningReplies(nextBoard, aiPlayer.opponent());
                if (threatReplies < minimumThreatReplies) {
                    minimumThreatReplies = threatReplies;
                    safestMoves.clear();
                    safestMoves.add(move);
                } else if (threatReplies == minimumThreatReplies) {
                    safestMoves.add(move);
                }
            }

            if (!safestMoves.isEmpty() && minimumThreatReplies < currentOpponentThreats) {
                return safestMoves.get(random.nextInt(safestMoves.size()));
            }
        }

        List<Integer> pressureBlocks = findOpenEndedThreatBlocks(board, aiPlayer.opponent());
        if (!pressureBlocks.isEmpty()) {
            return shortlistMoves(board, pressureBlocks, aiPlayer, 1).get(0);
        }

        return null;
    }

    private boolean isStrongGuardrailMode() {
        return !artificialStupidityEnabled;
    }

    private List<Integer> findOpenEndedThreatBlocks(Board board, Player player) {
        int targetLength = Math.max(2, board.getWinLength() - 2);
        java.util.Map<Integer, Integer> blockFrequency = new java.util.HashMap<>();
        int boardSize = board.getSize();

        for (int row = 0; row < boardSize; row++) {
            for (int column = 0; column < boardSize; column++) {
                for (int[] direction : DIRECTIONS) {
                    int endRow = row + direction[0] * (targetLength + 1);
                    int endColumn = column + direction[1] * (targetLength + 1);
                    if (endRow < 0 || endRow >= boardSize || endColumn < 0 || endColumn >= boardSize) {
                        continue;
                    }
                    int startIndex = row * boardSize + column;
                    int endIndex = endRow * boardSize + endColumn;
                    if (board.getCell(startIndex) != Player.EMPTY || board.getCell(endIndex) != Player.EMPTY) {
                        continue;
                    }

                    boolean matchingThreat = true;
                    for (int offset = 1; offset <= targetLength; offset++) {
                        int nextRow = row + direction[0] * offset;
                        int nextColumn = column + direction[1] * offset;
                        if (board.getCell(nextRow * boardSize + nextColumn) != player) {
                            matchingThreat = false;
                            break;
                        }
                    }

                    if (matchingThreat) {
                        blockFrequency.merge(startIndex, 1, Integer::sum);
                        blockFrequency.merge(endIndex, 1, Integer::sum);
                    }
                }
            }
        }

        return blockFrequency.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .map(java.util.Map.Entry::getKey)
                .toList();
    }

    private int countImmediateWinningReplies(Board board, Player player) {
        int threats = 0;
        for (int move : board.getAvailableMoves()) {
            if (wouldWinAfterMove(board, move, player)) {
                threats++;
            }
        }
        return threats;
    }

    /**
     * Samples a move using a softmax distribution over child quality.
     *
     * <p>The weight blends average reward and visit share so the choice still
     * reflects what the search discovered.
     */
    private MctsNode sampleSoftmax(List<MctsNode> children, double temperature, int rootVisits) {
        double[] weights = new double[children.size()];
        double weightSum = 0.0;

        for (int i = 0; i < children.size(); i++) {
            MctsNode child = children.get(i);
            double visitRatio = rootVisits == 0 ? 0.0 : (double) child.getVisitCount() / rootVisits;
            // Average score says how good the move looked; visit ratio says how
            // strongly the tree kept returning to it.
            double blendedScore = child.getAverageScore() * 0.7 + visitRatio * 0.3;
            double weight = Math.exp(blendedScore / temperature);
            weights[i] = weight;
            weightSum += weight;
        }

        double pick = random.nextDouble() * weightSum;
        double cumulative = 0.0;
        for (int i = 0; i < children.size(); i++) {
            cumulative += weights[i];
            if (pick <= cumulative) {
                return children.get(i);
            }
        }

        return children.get(children.size() - 1);
    }

    /**
     * Computes how much controlled imperfection should remain at the current
     * simulation budget.
     *
     * <p>Lower simulation counts produce a higher value, which leads to more
     * variety and more human-beatable play.
     */
    private double calculateImperfectionFactor(Board board) {
        if (!artificialStupidityEnabled) {
            return 0.0;
        }
        int clamped = Math.max(BASELINE_SIMULATIONS, Math.min(MAX_TUNED_SIMULATIONS, simulationCount));
        double normalized = (double) (clamped - BASELINE_SIMULATIONS)
                / (MAX_TUNED_SIMULATIONS - BASELINE_SIMULATIONS);
        double strength = Math.pow(normalized, 0.65);
        double sizeScaling = Math.min(1.0, 4.0 / board.getSize());
        double baselineImperfection = 0.55 * (1.0 - strength) * sizeScaling;
        return baselineImperfection * artificialStupidityLevel.getImperfectionMultiplier();
    }

    private int chooseRolloutMove(Board board) {
        Player currentPlayer = board.getCurrentPlayer();
        List<Integer> moves = board.getAvailableMoves();

        for (int move : moves) {
            if (wouldWinAfterMove(board, move, currentPlayer)) {
                return move;
            }
        }

        for (int move : moves) {
            if (wouldWinAfterMove(board, move, currentPlayer.opponent())) {
                return move;
            }
        }

        if (moves.size() == 1) {
            return moves.get(0);
        }

        List<Integer> rolloutCandidates = collectInterestingMoves(board, moves, ROLLOUT_CANDIDATE_LIMIT);
        List<Integer> topMoves = new java.util.ArrayList<>();
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int move : rolloutCandidates) {
            double score = evaluateRolloutMove(board, move, currentPlayer);
            if (score > bestScore) {
                bestScore = score;
                topMoves.clear();
                topMoves.add(move);
            } else if (score == bestScore) {
                topMoves.add(move);
            }
        }

        if (!topMoves.isEmpty() && random.nextDouble() < resolveRolloutGreediness()) {
            return topMoves.get(random.nextInt(topMoves.size()));
        }

        List<Integer> shortlist = shortlistMoves(board, rolloutCandidates, currentPlayer, Math.min(4, rolloutCandidates.size()));
        return shortlist.get(random.nextInt(shortlist.size()));
    }

    private MctsNode expandPromisingChild(MctsNode node) {
        List<Integer> untriedMoves = node.getUntriedMoves();
        if (untriedMoves.size() == 1) {
            return node.expandMove(untriedMoves.get(0));
        }
        List<Integer> candidateMoves = sampleMoves(untriedMoves, EXPANSION_SAMPLE_LIMIT);
        int chosenMove = shortlistMoves(node.getBoard(), candidateMoves, node.getBoard().getCurrentPlayer(), 1).get(0);
        return node.expandMove(chosenMove);
    }

    private List<Integer> sampleMoves(List<Integer> moves, int sampleLimit) {
        if (moves.size() <= sampleLimit) {
            return moves;
        }
        List<Integer> shuffled = new java.util.ArrayList<>(moves);
        java.util.Collections.shuffle(shuffled, random);
        return shuffled.subList(0, sampleLimit);
    }

    private List<Integer> collectInterestingMoves(Board board, List<Integer> moves, int limit) {
        List<Integer> localMoves = new java.util.ArrayList<>();
        int size = board.getSize();

        for (int move : moves) {
            int row = move / size;
            int column = move % size;
            if (hasNeighboringStone(board, row, column, 1)) {
                localMoves.add(move);
            }
        }

        if (localMoves.isEmpty()) {
            return shortlistMoves(board, moves, board.getCurrentPlayer(), Math.min(limit, moves.size()));
        }

        if (localMoves.size() <= limit) {
            return localMoves;
        }

        return shortlistMoves(board, localMoves, board.getCurrentPlayer(), limit);
    }

    private boolean hasNeighboringStone(Board board, int row, int column, int radius) {
        int size = board.getSize();
        for (int rowOffset = -radius; rowOffset <= radius; rowOffset++) {
            for (int columnOffset = -radius; columnOffset <= radius; columnOffset++) {
                if (rowOffset == 0 && columnOffset == 0) {
                    continue;
                }
                int nextRow = row + rowOffset;
                int nextColumn = column + columnOffset;
                if (nextRow < 0 || nextRow >= size || nextColumn < 0 || nextColumn >= size) {
                    continue;
                }
                if (board.getCell(nextRow * size + nextColumn) != Player.EMPTY) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Integer> shortlistMoves(Board board, List<Integer> moves, Player mover, int limit) {
        return moves.stream()
                .sorted((left, right) -> Double.compare(
                        evaluateStrategicMove(board, right, mover),
                        evaluateStrategicMove(board, left, mover)))
                .limit(Math.max(1, limit))
                .toList();
    }

    private double evaluateStrategicMove(Board board, int move, Player mover) {
        if (wouldWinAfterMove(board, move, mover)) {
            return 1_000_000.0;
        }
        if (wouldWinAfterMove(board, move, mover.opponent())) {
            return 900_000.0;
        }

        double score = evaluateCenterPreference(board, move) * 1.8;
        score += evaluateNeighborPressure(board, move, mover) * 2.4;
        score += evaluateDirectionalPotential(board, move, mover) * 20.0;
        score += evaluateDirectionalPotential(board, move, mover.opponent()) * 17.0;
        return score;
    }

    private double evaluateRolloutMove(Board board, int move, Player mover) {
        if (wouldWinAfterMove(board, move, mover)) {
            return 1_000_000.0;
        }
        if (wouldWinAfterMove(board, move, mover.opponent())) {
            return 900_000.0;
        }
        double score = evaluateCenterPreference(board, move) * 1.2;
        score += evaluateNeighborPressure(board, move, mover) * 2.0;
        score += evaluateDirectionalPotential(board, move, mover) * 8.0;
        score += evaluateDirectionalPotential(board, move, mover.opponent()) * 6.0;
        return score;
    }

    private int evaluateCenterPreference(Board board, int move) {
        int boardDimension = board.getSize();
        int row = move / boardDimension;
        int column = move % boardDimension;
        double center = (boardDimension - 1) / 2.0;
        double distance = Math.abs(row - center) + Math.abs(column - center);
        return (int) Math.round((boardDimension * 2) - distance);
    }

    private int evaluateNeighborPressure(Board board, int move, Player mover) {
        int boardDimension = board.getSize();
        int row = move / boardDimension;
        int column = move % boardDimension;
        int score = 0;

        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                if (rowOffset == 0 && columnOffset == 0) {
                    continue;
                }
                int nextRow = row + rowOffset;
                int nextColumn = column + columnOffset;
                if (nextRow < 0 || nextRow >= boardDimension || nextColumn < 0 || nextColumn >= boardDimension) {
                    continue;
                }
                Player neighbor = board.getCell(nextRow * boardDimension + nextColumn);
                if (neighbor == mover) {
                    score += 6;
                } else if (neighbor == mover.opponent()) {
                    score += 3;
                }
            }
        }

        if (score == 0) {
            return 1;
        }
        return score;
    }

    private double evaluateDirectionalPotential(Board board, int move, Player player) {
        int boardDimension = board.getSize();
        int row = move / boardDimension;
        int column = move % boardDimension;
        double total = 0.0;

        for (int[] direction : DIRECTIONS) {
            int leftCount = countConsecutive(board, row, column, -direction[0], -direction[1], player);
            int rightCount = countConsecutive(board, row, column, direction[0], direction[1], player);
            int openEnds = countOpenEnds(board, row, column, direction[0], direction[1], leftCount, rightCount);
            int streakLength = 1 + leftCount + rightCount;
            total += scoreStreak(streakLength, openEnds, board.getWinLength());
        }

        return total;
    }

    private int countConsecutive(Board board, int startRow, int startColumn, int rowStep, int columnStep, Player player) {
        int boardDimension = board.getSize();
        int row = startRow + rowStep;
        int column = startColumn + columnStep;
        int count = 0;

        while (row >= 0 && row < boardDimension && column >= 0 && column < boardDimension) {
            Player cell = board.getCell(row * boardDimension + column);
            if (cell != player) {
                break;
            }
            count++;
            row += rowStep;
            column += columnStep;
        }
        return count;
    }

    private int countOpenEnds(Board board,
                              int row,
                              int column,
                              int rowStep,
                              int columnStep,
                              int negativeCount,
                              int positiveCount) {
        int openEnds = 0;
        if (isOpenEnd(board, row - rowStep * (negativeCount + 1), column - columnStep * (negativeCount + 1))) {
            openEnds++;
        }
        if (isOpenEnd(board, row + rowStep * (positiveCount + 1), column + columnStep * (positiveCount + 1))) {
            openEnds++;
        }
        return openEnds;
    }

    private boolean isOpenEnd(Board board, int row, int column) {
        int boardDimension = board.getSize();
        if (row < 0 || row >= boardDimension || column < 0 || column >= boardDimension) {
            return false;
        }
        return board.getCell(row * boardDimension + column) == Player.EMPTY;
    }

    private double scoreStreak(int streakLength, int openEnds, int winLength) {
        if (streakLength >= winLength) {
            return 500_000.0;
        }
        double base = Math.pow(8.0, streakLength);
        if (openEnds == 2) {
            base *= 2.3;
        } else if (openEnds == 1) {
            base *= 1.15;
        } else {
            base *= 0.35;
        }
        if (streakLength == winLength - 1 && openEnds > 0) {
            base *= 8.0;
        } else if (streakLength == winLength - 2 && openEnds == 2) {
            base *= 3.5;
        }
        return base;
    }

    private double resolveRolloutGreediness() {
        if (!artificialStupidityEnabled) {
            return 0.96;
        }
        return switch (artificialStupidityLevel) {
            case SUPER_LOW -> 0.94;
            case LOW -> 0.90;
            case MEDIUM -> 0.82;
            case HIGH -> 0.72;
            case EXTRA_HIGH -> 0.60;
        };
    }

    private int calculateRolloutDepthLimit(Board board) {
        int base = Math.max(board.getWinLength() * 2, 8);
        return Math.min(base, 12);
    }

    private long calculateTimeBudgetNanos(Board board) {
        if (board.getSize() <= 3) {
            return MAX_TIME_BUDGET_MS * 1_000_000L;
        }
        long budgetMs = 1_200L
                + (board.getWinLength() * 250L)
                + Math.min(1_500L, simulationCount / 10L);
        return Math.min(MAX_TIME_BUDGET_MS, budgetMs) * 1_000_000L;
    }

    private Player estimateRolloutWinner(Board board) {
        double xPressure = estimateBoardPressure(board, Player.X);
        double oPressure = estimateBoardPressure(board, Player.O);
        double difference = xPressure - oPressure;
        if (Math.abs(difference) < 20.0) {
            return Player.EMPTY;
        }
        return difference > 0 ? Player.X : Player.O;
    }

    private double estimateBoardPressure(Board board, Player player) {
        double total = 0.0;
        int size = board.getSize();
        for (int index = 0; index < board.getCellCount(); index++) {
            if (board.getCell(index) != player) {
                continue;
            }
            int row = index / size;
            int column = index % size;
            for (int[] direction : DIRECTIONS) {
                int negativeCount = countConsecutive(board, row, column, -direction[0], -direction[1], player);
                int positiveCount = countConsecutive(board, row, column, direction[0], direction[1], player);
                int streakLength = 1 + negativeCount + positiveCount;
                int openEnds = countOpenEnds(board, row, column, direction[0], direction[1], negativeCount, positiveCount);
                total += scoreStreak(streakLength, openEnds, board.getWinLength());
            }
        }
        return total;
    }

    private boolean wouldWinAfterMove(Board board, int move, Player player) {
        int boardDimension = board.getSize();
        int row = move / boardDimension;
        int column = move % boardDimension;
        int winLength = board.getWinLength();

        return countDirection(board, row, column, 0, 1, player) >= winLength
                || countDirection(board, row, column, 1, 0, player) >= winLength
                || countDirection(board, row, column, 1, 1, player) >= winLength
                || countDirection(board, row, column, 1, -1, player) >= winLength;
    }

    private int countDirection(Board board, int row, int column, int rowStep, int columnStep, Player player) {
        return 1
                + countOneSide(board, row, column, rowStep, columnStep, player)
                + countOneSide(board, row, column, -rowStep, -columnStep, player);
    }

    private int countOneSide(Board board, int startRow, int startColumn, int rowStep, int columnStep, Player player) {
        int boardDimension = board.getSize();
        int count = 0;
        int row = startRow + rowStep;
        int column = startColumn + columnStep;

        while (row >= 0 && row < boardDimension && column >= 0 && column < boardDimension) {
            int index = row * boardDimension + column;
            Player cell = board.getCell(index);
            if (cell != player) {
                break;
            }
            count++;
            row += rowStep;
            column += columnStep;
        }
        return count;
    }

    private String detectGamePhase(Board board) {
        int occupied = board.getCellCount() - board.getAvailableMoves().size();
        int winLength = board.getWinLength();
        if (occupied < winLength * 2) {
            return "Opening";
        }
        if (occupied < winLength * 4) {
            return "Midgame";
        }
        return "Endgame";
    }

    private List<MoveCandidate> mapCandidates(Board board, List<MctsNode> rankedChildren) {
        return rankedChildren.stream()
                .limit(5)
                .map(child -> new MoveCandidate(
                        child.getMove(),
                        child.getMove() / board.getSize(),
                        child.getMove() % board.getSize(),
                        child.getVisitCount(),
                        child.getAverageScore(),
                        child.getAverageScore()))
                .toList();
    }

    private AiMoveTelemetry createSingleMoveTelemetry(Board board, int move, long startedAtNanos) {
        long responseTimeMillis = Math.max(1L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
        int legalMoves = board.getAvailableMoves().size();
        return new AiMoveTelemetry(
                buildTelemetryName(),
                false,
                move,
                move / board.getSize(),
                move % board.getSize(),
                System.currentTimeMillis(),
                responseTimeMillis,
                board.getSize(),
                board.getWinLength(),
                legalMoves,
                board.getCellCount() - legalMoves,
                detectGamePhase(board),
                0L,
                0L,
                0,
                1.0,
                1L,
                0.0,
                0.0,
                0.0,
                1.0,
                List.of(new MoveCandidate(move, move / board.getSize(), move % board.getSize(), 1L, 1.0, 1.0)),
                null,
                null);
    }

    private record RolloutResult(Player winner, int rolloutLength, long durationNanos) {
    }

    private String buildTelemetryName() {
        return artificialStupidityEnabled
                ? "MCTS [" + artificialStupidityLevel.getDisplayName() + "]"
                : "MCTS [AS Off]";
    }

    private static final class SearchStats {

        private long selectionSteps;
        private long expandedNodes;
        private long completedSimulations;
        private long totalRolloutMoves;
        private long totalRolloutTimeNanos;
    }
}
