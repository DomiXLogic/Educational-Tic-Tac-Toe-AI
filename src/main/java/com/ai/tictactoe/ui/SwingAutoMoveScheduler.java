package com.ai.tictactoe.ui;

import com.ai.tictactoe.controller.AutoMoveScheduler;
import javax.swing.Timer;

public final class SwingAutoMoveScheduler implements AutoMoveScheduler {

    private Timer timer;

    @Override
    public void schedule(int delayMillis, Runnable action) {
        cancel();
        timer = new Timer(delayMillis, event -> action.run());
        timer.setRepeats(false);
        timer.start();
    }

    @Override
    public void cancel() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }
}
