package com.ai.tictactoe.ai;

import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.Player;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class MctsNode {

    private final Board board;
    private final MctsNode parent;
    private final List<MctsNode> children;
    private final List<Integer> untriedMoves;
    private final int move;
    private final Player playerJustMoved;

    private int visitCount;
    private double totalScore;

    public MctsNode(Board board, MctsNode parent, int move, Player playerJustMoved) {
        this.board = board;
        this.parent = parent;
        this.move = move;
        this.playerJustMoved = playerJustMoved;
        this.children = new ArrayList<>();
        this.untriedMoves = new ArrayList<>(board.getAvailableMoves());
    }

    public Board getBoard() {
        return board;
    }

    public MctsNode getParent() {
        return parent;
    }

    public List<MctsNode> getChildren() {
        return children;
    }

    public int getMove() {
        return move;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public double getAverageScore() {
        return visitCount == 0 ? 0.0 : totalScore / visitCount;
    }

    public boolean hasUntriedMoves() {
        return !untriedMoves.isEmpty();
    }

    public boolean isFullyExpanded() {
        return untriedMoves.isEmpty();
    }

    public List<Integer> getUntriedMoves() {
        return List.copyOf(untriedMoves);
    }

    public MctsNode expand(Random random) {
        int moveIndex = random.nextInt(untriedMoves.size());
        int nextMove = untriedMoves.remove(moveIndex);
        return expandMove(nextMove);
    }

    public MctsNode expandMove(int nextMove) {
        if (!untriedMoves.remove(Integer.valueOf(nextMove))) {
            throw new IllegalArgumentException("Move " + nextMove + " is not available for expansion.");
        }
        Player mover = board.getCurrentPlayer();
        MctsNode child = new MctsNode(board.makeMove(nextMove), this, nextMove, mover);
        children.add(child);
        return child;
    }

    public MctsNode selectChildByUct(double explorationConstant) {
        double parentVisits = Math.max(1, visitCount);
        return children.stream()
                .max(Comparator.comparingDouble(child -> child.uctValue(parentVisits, explorationConstant)))
                .orElseThrow(() -> new IllegalStateException("No child available for UCT selection."));
    }

    public MctsNode bestChildByVisits() {
        return children.stream()
                .max(Comparator.comparingInt(MctsNode::getVisitCount)
                        .thenComparingDouble(MctsNode::getAverageScore))
                .orElseThrow(() -> new IllegalStateException("No child available after MCTS search."));
    }

    public void updateStats(Player winner) {
        visitCount++;
        if (winner == Player.EMPTY) {
            totalScore += 0.5;
        } else if (winner == playerJustMoved) {
            totalScore += 1.0;
        }
    }

    private double uctValue(double parentVisits, double explorationConstant) {
        if (visitCount == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return getAverageScore()
                + explorationConstant * Math.sqrt(Math.log(parentVisits) / visitCount);
    }
}
