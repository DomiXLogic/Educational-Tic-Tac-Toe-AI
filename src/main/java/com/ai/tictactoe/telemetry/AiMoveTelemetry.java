package com.ai.tictactoe.telemetry;

import java.util.List;

public record AiMoveTelemetry(
        String algorithmName,
        boolean exactSearch,
        int chosenMove,
        int chosenRow,
        int chosenColumn,
        long timestampMillis,
        long responseTimeMillis,
        int boardDimension,
        int winLength,
        int legalMoves,
        int occupiedCells,
        String gamePhase,
        long nodesExplored,
        long alphaBetaCutoffs,
        int maxDepthReached,
        double heuristicScore,
        long simulations,
        double simulationsPerSecond,
        double averageRolloutLength,
        double averageRolloutTimeMillis,
        double bestMoveVisitRatio,
        List<MoveCandidate> candidateMoves,
        ProcessSnapshot processBefore,
        ProcessSnapshot processAfter) {

    public AiMoveTelemetry withProcessSnapshots(ProcessSnapshot processBefore, ProcessSnapshot processAfter) {
        return new AiMoveTelemetry(
                algorithmName,
                exactSearch,
                chosenMove,
                chosenRow,
                chosenColumn,
                timestampMillis,
                responseTimeMillis,
                boardDimension,
                winLength,
                legalMoves,
                occupiedCells,
                gamePhase,
                nodesExplored,
                alphaBetaCutoffs,
                maxDepthReached,
                heuristicScore,
                simulations,
                simulationsPerSecond,
                averageRolloutLength,
                averageRolloutTimeMillis,
                bestMoveVisitRatio,
                candidateMoves,
                processBefore,
                processAfter);
    }
}
