package com.ai.tictactoe.telemetry;

public record MoveCandidate(
        int moveIndex,
        int row,
        int column,
        long visits,
        double averageScore,
        double evaluationScore) {
}
