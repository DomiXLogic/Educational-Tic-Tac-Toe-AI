package com.ai.tictactoe.controller;

public interface AutoMoveScheduler {

    void schedule(int delayMillis, Runnable action);

    void cancel();
}
