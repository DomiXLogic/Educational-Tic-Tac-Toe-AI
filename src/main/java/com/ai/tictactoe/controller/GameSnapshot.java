package com.ai.tictactoe.controller;

import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.GameMode;
import com.ai.tictactoe.model.ScoreBoard;
import com.ai.tictactoe.model.StartMode;

public record GameSnapshot(
        Board board,
        GameMode gameMode,
        StartMode startMode,
        int mctsSimulationCount,
        ScoreBoard scoreBoard,
        boolean boardInputEnabled,
        boolean aiThinking,
        boolean gameOver,
        String statusMessage) {
}
