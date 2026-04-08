package com.ai.tictactoe.ai;

import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.Player;

public final class MinimaxStrategy implements ComputerStrategy {

    @Override
    public int chooseMove(Board board, Player aiPlayer) {
        if (board.getCurrentPlayer() != aiPlayer) {
            throw new IllegalArgumentException("Minimax called when it is not " + aiPlayer + "'s turn.");
        }

        int bestMove = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int move : board.getAvailableMoves()) {
            Board nextBoard = board.makeMove(move);
            int score = minimax(nextBoard, aiPlayer, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        if (bestMove < 0) {
            throw new IllegalStateException("No legal move available for Minimax.");
        }

        return bestMove;
    }

    private int minimax(Board board, Player aiPlayer, int depth, int alpha, int beta) {
        if (board.isTerminal()) {
            return evaluate(board, aiPlayer, depth);
        }

        boolean maximizing = board.getCurrentPlayer() == aiPlayer;
        if (maximizing) {
            int best = Integer.MIN_VALUE;
            for (int move : board.getAvailableMoves()) {
                best = Math.max(best, minimax(board.makeMove(move), aiPlayer, depth + 1, alpha, beta));
                alpha = Math.max(alpha, best);
                if (beta <= alpha) {
                    break;
                }
            }
            return best;
        }

        int best = Integer.MAX_VALUE;
        for (int move : board.getAvailableMoves()) {
            best = Math.min(best, minimax(board.makeMove(move), aiPlayer, depth + 1, alpha, beta));
            beta = Math.min(beta, best);
            if (beta <= alpha) {
                break;
            }
        }
        return best;
    }

    private int evaluate(Board board, Player aiPlayer, int depth) {
        Player winner = board.getWinner();
        if (winner == aiPlayer) {
            return 10 - depth;
        }
        if (winner == aiPlayer.opponent()) {
            return depth - 10;
        }
        return 0;
    }
}
