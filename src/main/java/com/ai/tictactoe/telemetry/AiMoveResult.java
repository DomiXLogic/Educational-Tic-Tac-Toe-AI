package com.ai.tictactoe.telemetry;

public record AiMoveResult(
        int move,
        AiMoveTelemetry telemetry) {
}
