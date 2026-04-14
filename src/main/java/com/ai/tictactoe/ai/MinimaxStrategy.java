package com.ai.tictactoe.ai;

import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.BoardSize;
import com.ai.tictactoe.model.Player;
import com.ai.tictactoe.telemetry.AiMoveResult;
import com.ai.tictactoe.telemetry.AiMoveTelemetry;
import com.ai.tictactoe.telemetry.MoveCandidate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MinimaxStrategy implements ComputerStrategy {

    private static final int UNLIMITED_DEPTH = Integer.MAX_VALUE;

    @Override
    public AiMoveResult chooseMove(Board board, Player aiPlayer) {
        if (board.getCurrentPlayer() != aiPlayer) {
            throw new IllegalArgumentException("Minimax called when it is not " + aiPlayer + "'s turn.");
        }

        long startedAtNanos = System.nanoTime();
        SearchProfile profile = SearchProfile.forBoard(board.getBoardSize());
        SearchStats stats = new SearchStats();
        List<RootCandidate> candidates = new ArrayList<>();
        int bestMove = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int move : orderMoves(board, aiPlayer, true, 0, profile)) {
            Board nextBoard = board.makeMove(move);
            int score = minimax(nextBoard, aiPlayer, 1, profile, Integer.MIN_VALUE, Integer.MAX_VALUE, stats);
            candidates.add(new RootCandidate(move, score));
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        if (bestMove < 0) {
            throw new IllegalStateException("No legal move available for Minimax.");
        }

        long responseTimeMillis = Math.max(1L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
        AiMoveTelemetry telemetry = new AiMoveTelemetry(
                "Minimax",
                profile.isExact(),
                bestMove,
                bestMove / board.getSize(),
                bestMove % board.getSize(),
                System.currentTimeMillis(),
                responseTimeMillis,
                board.getSize(),
                board.getWinLength(),
                board.getAvailableMoves().size(),
                board.getCellCount() - board.getAvailableMoves().size(),
                detectGamePhase(board),
                stats.nodesExplored,
                stats.alphaBetaCutoffs,
                stats.maxDepthReached,
                bestScore,
                0L,
                0.0,
                0.0,
                0.0,
                0.0,
                mapCandidates(board, candidates),
                null,
                null);
        return new AiMoveResult(bestMove, telemetry);
    }

    private int minimax(Board board,
                        Player aiPlayer,
                        int depth,
                        SearchProfile profile,
                        int alpha,
                        int beta,
                        SearchStats stats) {
        stats.nodesExplored++;
        stats.maxDepthReached = Math.max(stats.maxDepthReached, depth);

        if (board.isTerminal()) {
            return evaluateTerminal(board, aiPlayer, depth);
        }
        if (!profile.isExact() && depth >= profile.maxDepth()) {
            return evaluateHeuristic(board, aiPlayer);
        }

        boolean maximizing = board.getCurrentPlayer() == aiPlayer;
        List<Integer> orderedMoves = orderMoves(board, aiPlayer, maximizing, depth, profile);
        if (maximizing) {
            int best = Integer.MIN_VALUE;
            for (int move : orderedMoves) {
                best = Math.max(best, minimax(board.makeMove(move), aiPlayer, depth + 1, profile, alpha, beta, stats));
                alpha = Math.max(alpha, best);
                if (beta <= alpha) {
                    stats.alphaBetaCutoffs++;
                    break;
                }
            }
            return best;
        }

        int best = Integer.MAX_VALUE;
        for (int move : orderedMoves) {
            best = Math.min(best, minimax(board.makeMove(move), aiPlayer, depth + 1, profile, alpha, beta, stats));
            beta = Math.min(beta, best);
            if (beta <= alpha) {
                stats.alphaBetaCutoffs++;
                break;
            }
        }
        return best;
    }

    private int evaluateTerminal(Board board, Player aiPlayer, int depth) {
        Player winner = board.getWinner();
        if (winner == aiPlayer) {
            return 10_000 - depth;
        }
        if (winner == aiPlayer.opponent()) {
            return depth - 10_000;
        }
        return 0;
    }

    private List<Integer> orderMoves(Board board,
                                     Player aiPlayer,
                                     boolean maximizing,
                                     int depth,
                                     SearchProfile profile) {
        List<Integer> orderedMoves = new ArrayList<>(board.getAvailableMoves());
        Comparator<Integer> comparator = Comparator.comparingInt(move ->
                evaluateMove(board, move, aiPlayer));
        orderedMoves.sort(maximizing ? comparator.reversed() : comparator);

        int candidateLimit = profile.candidateLimit(depth);
        if (candidateLimit < orderedMoves.size()) {
            return new ArrayList<>(orderedMoves.subList(0, candidateLimit));
        }
        return orderedMoves;
    }

    private int evaluateMove(Board board, int move, Player aiPlayer) {
        Board nextBoard = board.makeMove(move);
        if (nextBoard.isTerminal()) {
            return evaluateTerminal(nextBoard, aiPlayer, 0);
        }
        return evaluateHeuristic(nextBoard, aiPlayer);
    }

    private int evaluateHeuristic(Board board, Player aiPlayer) {
        int score = 0;
        for (int[] line : board.getPotentialWinningLines()) {
            score += evaluateLine(board, aiPlayer, line);
        }
        score += evaluateCenterControl(board, aiPlayer);
        return score;
    }

    private int evaluateLine(Board board, Player aiPlayer, int[] line) {
        int aiCount = 0;
        int opponentCount = 0;

        for (int cellIndex : line) {
            Player cell = board.getCell(cellIndex);
            if (cell == aiPlayer) {
                aiCount++;
            } else if (cell == aiPlayer.opponent()) {
                opponentCount++;
            }
        }
        return scoreCounts(aiCount, opponentCount, board.getBoardSize());
    }

    private int evaluateCenterControl(Board board, Player aiPlayer) {
        int boardDimension = board.getSize();
        int centerStart = (boardDimension - 2) / 2;
        int centerEnd = boardDimension % 2 == 0 ? centerStart + 1 : centerStart;
        int score = 0;

        for (int row = centerStart; row <= centerEnd; row++) {
            for (int column = centerStart; column <= centerEnd; column++) {
                Player cell = board.getCell(row * boardDimension + column);
                if (cell == aiPlayer) {
                    score += 6;
                } else if (cell == aiPlayer.opponent()) {
                    score -= 6;
                }
            }
        }
        return score;
    }

    private int scoreCounts(int aiCount, int opponentCount, BoardSize boardSize) {
        if (aiCount > 0 && opponentCount > 0) {
            return 0;
        }
        if (aiCount == 0 && opponentCount == 0) {
            return 0;
        }

        int weight = lineWeight(Math.max(aiCount, opponentCount), boardSize.getWinLength());
        return aiCount > 0 ? weight : -weight;
    }

    private int lineWeight(int markersInLine, int lineLength) {
        int weight = 1;
        int base = lineLength + 1;
        for (int i = 0; i < markersInLine; i++) {
            weight *= base;
        }
        return weight;
    }

    private String detectGamePhase(Board board) {
        double occupiedRatio = (double) (board.getCellCount() - board.getAvailableMoves().size()) / board.getCellCount();
        if (occupiedRatio < 0.25) {
            return "Opening";
        }
        if (occupiedRatio < 0.70) {
            return "Midgame";
        }
        return "Endgame";
    }

    private List<MoveCandidate> mapCandidates(Board board, List<RootCandidate> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparingInt(RootCandidate::score).reversed())
                .limit(5)
                .map(candidate -> new MoveCandidate(
                        candidate.move(),
                        candidate.move() / board.getSize(),
                        candidate.move() % board.getSize(),
                        0L,
                        0.0,
                        candidate.score()))
                .toList();
    }

    private record SearchProfile(boolean exact, int maxDepth, int rootCandidateLimit, int deeperCandidateLimit) {

        static SearchProfile forBoard(BoardSize boardSize) {
            return switch (boardSize) {
                case THREE -> new SearchProfile(true, UNLIMITED_DEPTH, Integer.MAX_VALUE, Integer.MAX_VALUE);
                default -> new SearchProfile(false,
                        boardSize.getHeuristicDepth(),
                        boardSize.getRootCandidateLimit(),
                        boardSize.getDeeperCandidateLimit());
            };
        }

        boolean isExact() {
            return exact;
        }

        int candidateLimit(int depth) {
            return depth == 0 ? rootCandidateLimit : deeperCandidateLimit;
        }
    }

    private record RootCandidate(int move, int score) {
    }

    private static final class SearchStats {

        private long nodesExplored;
        private long alphaBetaCutoffs;
        private int maxDepthReached;
    }
}
