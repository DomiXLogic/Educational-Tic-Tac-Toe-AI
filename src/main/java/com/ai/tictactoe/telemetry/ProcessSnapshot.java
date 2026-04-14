package com.ai.tictactoe.telemetry;

public record ProcessSnapshot(
        long timestampMillis,
        double processCpuLoadPercent,
        long processCpuTimeNanos,
        long heapUsedBytes,
        long heapCommittedBytes,
        long heapMaxBytes,
        int threadCount,
        long gcCount,
        long gcTimeMillis) {
}
