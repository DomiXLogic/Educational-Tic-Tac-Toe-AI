package com.ai.tictactoe.ai;

import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.Player;
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

    private final Random random;
    private int simulationCount;

    public MctsStrategy(int simulationCount) {
        this(simulationCount, new Random());
    }

    MctsStrategy(int simulationCount, Random random) {
        this.simulationCount = simulationCount;
        this.random = random;
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

    /**
     * Runs an MCTS search from the current board and returns the move chosen
     * for the AI player.
     */
    @Override
    public int chooseMove(Board board, Player aiPlayer) {
        if (board.getCurrentPlayer() != aiPlayer) {
            throw new IllegalArgumentException("MCTS called when it is not " + aiPlayer + "'s turn.");
        }

        List<Integer> availableMoves = board.getAvailableMoves();
        if (availableMoves.isEmpty()) {
            throw new IllegalStateException("No legal move available for MCTS.");
        }
        if (availableMoves.size() == 1) {
            return availableMoves.get(0);
        }

        MctsNode root = new MctsNode(board.copy(), null, -1, board.getCurrentPlayer().opponent());

        for (int simulation = 0; simulation < simulationCount; simulation++) {
            MctsNode node = root;

            // 1. Selection: follow the most promising UCT path while the node
            // is fully expanded and the game is not over.
            while (!node.getBoard().isTerminal() && node.isFullyExpanded() && !node.getChildren().isEmpty()) {
                node = node.selectChildByUct(EXPLORATION_CONSTANT);
            }

            // 2. Expansion: grow the tree by trying one unexpanded move.
            if (!node.getBoard().isTerminal() && node.hasUntriedMoves()) {
                node = node.expand(random);
            }

            // 3. Simulation / rollout: play random moves until the board ends.
            Player winner = rollout(node.getBoard());

            // 4. Backpropagation: send the result back through the visited path.
            backpropagate(node, winner);
        }

        // Standard MCTS would usually pick the most visited child directly.
        // This project applies a calibrated policy here so low budgets remain
        // educationally useful and visibly less perfect.
        return selectFinalMove(root);
    }

    /**
     * Plays a random game from the provided board until a terminal result is
     * reached.
     */
    private Player rollout(Board board) {
        Board rolloutBoard = board.copy();
        while (!rolloutBoard.isTerminal()) {
            List<Integer> moves = rolloutBoard.getAvailableMoves();
            int move = moves.get(random.nextInt(moves.size()));
            rolloutBoard = rolloutBoard.makeMove(move);
        }
        return rolloutBoard.getWinner();
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

    /**
     * Chooses the move shown to the player after search is complete.
     *
     * <p>At high simulation counts this behaves close to standard MCTS. At low
     * counts it intentionally adds controlled stochasticity so the AI is still
     * beatable and the difference from Minimax remains obvious in class.
     */
    private int selectFinalMove(MctsNode root) {
        List<MctsNode> children = root.getChildren().stream()
                .sorted((left, right) -> {
                    int visitCompare = Integer.compare(right.getVisitCount(), left.getVisitCount());
                    if (visitCompare != 0) {
                        return visitCompare;
                    }
                    return Double.compare(right.getAverageScore(), left.getAverageScore());
                })
                .toList();

        double imperfection = calculateImperfectionFactor();
        if (imperfection <= 0.05) {
            // At high strength, fall back to the strongest child directly.
            return children.get(0).getMove();
        }

        if (random.nextDouble() < imperfection * 0.40) {
            // A small random branch keeps low-strength settings visibly imperfect.
            return children.get(random.nextInt(children.size())).getMove();
        }

        // Most of the time we sample among strong candidates, not blindly.
        double temperature = 0.18 + imperfection * 1.35;
        return sampleSoftmax(children, temperature, root.getVisitCount());
    }

    /**
     * Samples a move using a softmax distribution over child quality.
     *
     * <p>The weight blends average reward and visit share so the choice still
     * reflects what the search discovered.
     */
    private int sampleSoftmax(List<MctsNode> children, double temperature, int rootVisits) {
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
                return children.get(i).getMove();
            }
        }

        return children.get(children.size() - 1).getMove();
    }

    /**
     * Computes how much controlled imperfection should remain at the current
     * simulation budget.
     *
     * <p>Lower simulation counts produce a higher value, which leads to more
     * variety and more human-beatable play.
     */
    private double calculateImperfectionFactor() {
        int clamped = Math.max(BASELINE_SIMULATIONS, Math.min(MAX_TUNED_SIMULATIONS, simulationCount));
        double normalized = (double) (clamped - BASELINE_SIMULATIONS)
                / (MAX_TUNED_SIMULATIONS - BASELINE_SIMULATIONS);
        double strength = Math.pow(normalized, 0.65);
        return 0.55 * (1.0 - strength);
    }
}
