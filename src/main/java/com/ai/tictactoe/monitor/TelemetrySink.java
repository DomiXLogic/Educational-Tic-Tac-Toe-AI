package com.ai.tictactoe.monitor;

import com.ai.tictactoe.telemetry.AiMoveTelemetry;

public interface TelemetrySink {

    void recordAiMove(long gameId, AiMoveTelemetry telemetry);

    void recordGameOutcome(long gameId, String outcome);
}
