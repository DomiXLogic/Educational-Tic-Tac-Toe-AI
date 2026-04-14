package com.ai.tictactoe.ui;

import com.ai.tictactoe.ai.ComputerStrategy;
import com.ai.tictactoe.controller.AiMoveExecutor;
import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.Player;
import com.ai.tictactoe.telemetry.AiMoveResult;
import com.ai.tictactoe.telemetry.ProcessSnapshot;
import com.ai.tictactoe.telemetry.ProcessTelemetry;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

public final class SwingAiMoveExecutor implements AiMoveExecutor {

    @Override
    public void execute(Board board,
                        Player aiPlayer,
                        ComputerStrategy strategy,
                        Consumer<AiMoveResult> onSuccess,
                        Consumer<Throwable> onFailure) {
        SwingWorker<AiMoveResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AiMoveResult doInBackground() {
                ProcessSnapshot before = ProcessTelemetry.capture();
                AiMoveResult result = strategy.chooseMove(board, aiPlayer);
                ProcessSnapshot after = ProcessTelemetry.capture();
                return new AiMoveResult(
                        result.move(),
                        result.telemetry().withProcessSnapshots(before, after));
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
