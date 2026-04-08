package com.ai.tictactoe.ui;

import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.Player;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public final class BoardPanel extends JPanel {

    private static final Color WINNING_COLOR = new Color(201, 236, 192);
    private static final Color WINNING_PULSE_COLOR = new Color(255, 214, 102);
    private static final int BUTTON_FONT_SIZE = 42;
    private static final int WIN_FONT_SIZE = 48;

    private final JButton[] cells;
    private final Font buttonFont;
    private final Font winningFont;
    private final Timer winAnimationTimer;
    private IntConsumer clickListener;
    private Set<Integer> animatedWinningIndexes;
    private boolean pulsePhase;

    public BoardPanel() {
        setLayout(new GridLayout(3, 3, 8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        cells = new JButton[9];
        buttonFont = new Font(Font.SANS_SERIF, Font.BOLD, BUTTON_FONT_SIZE);
        winningFont = buttonFont.deriveFont(Font.BOLD, WIN_FONT_SIZE);
        animatedWinningIndexes = Collections.emptySet();
        winAnimationTimer = new Timer(180, event -> {
            pulsePhase = !pulsePhase;
            updateAnimatedWinningCells();
        });

        for (int i = 0; i < cells.length; i++) {
            int index = i;
            JButton button = new JButton();
            button.setFont(buttonFont);
            button.setFocusPainted(false);
            button.addActionListener(event -> {
                if (clickListener != null) {
                    clickListener.accept(index);
                }
            });
            cells[i] = button;
            add(button);
        }
    }

    public void setClickListener(IntConsumer clickListener) {
        this.clickListener = clickListener;
    }

    public void render(Board board, boolean boardInputEnabled) {
        Set<Integer> winningIndexes = new HashSet<>();
        for (int index : board.getWinningLine()) {
            winningIndexes.add(index);
        }

        updateWinningAnimation(winningIndexes);

        for (int i = 0; i < cells.length; i++) {
            JButton button = cells[i];
            Player player = board.getCell(i);
            button.setText(player == Player.EMPTY ? "" : String.valueOf(player.getSymbol()));
            button.setEnabled(boardInputEnabled && board.isMoveValid(i));
            button.setFont(winningIndexes.contains(i) ? getWinningFont() : buttonFont);
            button.setBackground(resolveBackground(winningIndexes.contains(i)));
        }
    }

    private void updateWinningAnimation(Set<Integer> winningIndexes) {
        if (winningIndexes.isEmpty()) {
            stopWinningAnimation();
            return;
        }

        if (!winningIndexes.equals(animatedWinningIndexes)) {
            animatedWinningIndexes = Set.copyOf(winningIndexes);
            pulsePhase = false;
            if (!winAnimationTimer.isRunning()) {
                winAnimationTimer.start();
            }
        } else if (!winAnimationTimer.isRunning()) {
            winAnimationTimer.start();
        }
    }

    private void stopWinningAnimation() {
        animatedWinningIndexes = Collections.emptySet();
        pulsePhase = false;
        if (winAnimationTimer.isRunning()) {
            winAnimationTimer.stop();
        }
    }

    private void updateAnimatedWinningCells() {
        for (int i = 0; i < cells.length; i++) {
            JButton button = cells[i];
            boolean winningCell = animatedWinningIndexes.contains(i);
            button.setFont(winningCell ? getWinningFont() : buttonFont);
            button.setBackground(resolveBackground(winningCell));
        }
        repaint();
    }

    private Font getWinningFont() {
        return pulsePhase ? winningFont : buttonFont;
    }

    private Color resolveBackground(boolean winningCell) {
        if (!winningCell) {
            return getDefaultButtonColor();
        }
        return pulsePhase ? WINNING_PULSE_COLOR : WINNING_COLOR;
    }

    private Color getDefaultButtonColor() {
        Color color = UIManager.getColor("Button.background");
        return color != null ? color : new JButton().getBackground();
    }
}
