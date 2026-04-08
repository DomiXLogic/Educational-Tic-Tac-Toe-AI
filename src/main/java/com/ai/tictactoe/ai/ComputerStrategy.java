package com.ai.tictactoe.ai;

import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.Player;

public interface ComputerStrategy {

    int chooseMove(Board board, Player aiPlayer);
}
