package com.ai.tictactoe.ai;

import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.Player;
import com.ai.tictactoe.telemetry.AiMoveResult;

public interface ComputerStrategy {

    AiMoveResult chooseMove(Board board, Player aiPlayer);
}
