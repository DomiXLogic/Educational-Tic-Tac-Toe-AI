package com.ai.tictactoe.controller;

import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.ArtificialStupidityLevel;
import com.ai.tictactoe.model.BoardSize;
import com.ai.tictactoe.model.BotStrategyType;
import com.ai.tictactoe.model.GameMode;
import com.ai.tictactoe.model.ScoreBoard;
import com.ai.tictactoe.model.StartMode;

public record GameSnapshot(
        long gameId,
        Board board,
        BoardSize boardSize,
        GameMode gameMode,
        StartMode startMode,
        int mctsSimulationCount,
        boolean mctsArtificialStupidityEnabled,
        ArtificialStupidityLevel mctsStupidityLevel,
        BotStrategyType xBotStrategy,
        BotStrategyType oBotStrategy,
        int xMctsSimulationCount,
        int oMctsSimulationCount,
        boolean xMctsArtificialStupidityEnabled,
        boolean oMctsArtificialStupidityEnabled,
        ArtificialStupidityLevel xMctsStupidityLevel,
        ArtificialStupidityLevel oMctsStupidityLevel,
        int pcVsPcMoveDelayMillis,
        boolean pcVsPcRunning,
        int lastComputerMove,
        ScoreBoard scoreBoard,
        boolean boardInputEnabled,
        boolean aiThinking,
        boolean gameOver,
        String statusMessage) {
}
