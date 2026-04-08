package com.ai.tictactoe.ui;

import com.ai.tictactoe.ai.ComputerStrategy;
import com.ai.tictactoe.controller.AiMoveExecutor;
import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.Player;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

public final class SwingAiMoveExecutor implements AiMoveExecutor {

    @Override
    public void execute(Board board,
                        Player aiPlayer,
                        ComputerStrategy strategy,
                        Consumer<Integer> onSuccess,
                        Consumer<Throwable> onFailure) {
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                return strategy.chooseMove(board, aiPlayer);
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    onFailure.accept(exception);
                } catch (ExecutionException exception) {
                    onFailure.accept(exception.getCause() == null ? exception : exception.getCause());
                }
            }
        };
        worker.execute();
    }
}
