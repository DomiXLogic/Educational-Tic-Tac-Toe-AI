package com.ai.tictactoe;

import com.ai.tictactoe.ui.ThemeManager;
import com.ai.tictactoe.ui.MainFrame;
import javax.swing.SwingUtilities;

public final class AiTiTacToe {

    private AiTiTacToe() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ThemeManager.applyLightTheme();
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
