package com.ai.tictactoe.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.Window;
import javax.swing.SwingUtilities;

public final class ThemeManager {

    private ThemeManager() {
    }

    public static void applyLightTheme() {
        FlatMacLightLaf.setup();
    }

    public static void applyTheme(boolean darkMode, Window window) {
        if (darkMode) {
            FlatMacDarkLaf.setup();
        } else {
            FlatMacLightLaf.setup();
        }

        FlatLaf.updateUI();
        if (window != null) {
            SwingUtilities.updateComponentTreeUI(window);
            window.pack();
        }
    }
}
