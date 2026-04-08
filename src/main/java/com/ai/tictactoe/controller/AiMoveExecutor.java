package com.ai.tictactoe.controller;

import com.ai.tictactoe.ai.ComputerStrategy;
import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.Player;
import java.util.function.Consumer;

public interface AiMoveExecutor {

    void execute(Board board,
                 Player aiPlayer,
                 ComputerStrategy strategy,
                 Consumer<Integer> onSuccess,
                 Consumer<Throwable> onFailure);
}
