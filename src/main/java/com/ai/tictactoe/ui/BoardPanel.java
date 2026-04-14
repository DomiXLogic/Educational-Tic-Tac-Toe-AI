package com.ai.tictactoe.ui;

import com.ai.tictactoe.model.Board;
import com.ai.tictactoe.model.Player;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public final class BoardPanel extends JPanel {

    private static final Color WINNING_SYMBOL_COLOR = new Color(144, 238, 144);
    private static final Color X_COLOR = new Color(0x58, 0x56, 0xD6);
    private static final Color X_DARK_THEME_COLOR = new Color(0x8A, 0x88, 0xFF);
    private static final Color O_COLOR = new Color(0x98, 0x98, 0x00);
    private static final Color LAST_MOVE_FLASH_PRIMARY = new Color(0xFF, 0xD6, 0x5A, 220);
    private static final Color LAST_MOVE_FLASH_SECONDARY = new Color(0x7D, 0xE8, 0xFF, 200);
    private static final int LAST_MOVE_FLASH_STEPS = 8;

    private final List<BoardCellButton> cells;
    private final Timer winAnimationTimer;
    private final Timer lastMoveFlashTimer;
    private Font buttonFont;
    private Font winningFont;
    private IntConsumer clickListener;
    private Set<Integer> animatedWinningIndexes;
    private boolean pulsePhase;
    private int currentBoardSize;
    private int flashedCellIndex;
    private int flashStepsRemaining;
    private boolean flashPhase;
    private Boolean lastBoardInputEnabled;

    public BoardPanel() {
        setBorder(new EmptyBorder(8, 8, 8, 8));

        cells = new ArrayList<>();
        animatedWinningIndexes = Collections.emptySet();
        currentBoardSize = 0;
        flashedCellIndex = -1;
        lastBoardInputEnabled = null;
        winAnimationTimer = new Timer(180, event -> {
            pulsePhase = !pulsePhase;
            updateAnimatedWinningCells();
        });
        lastMoveFlashTimer = new Timer(120, event -> advanceLastMoveFlash());
    }

    public void setClickListener(IntConsumer clickListener) {
        this.clickListener = clickListener;
    }

    public void flashCell(int index) {
        if (index < 0 || index >= cells.size()) {
            return;
        }

        flashedCellIndex = index;
        flashStepsRemaining = LAST_MOVE_FLASH_STEPS;
        flashPhase = false;
        applyLastMoveFlashState();
        if (lastMoveFlashTimer.isRunning()) {
            lastMoveFlashTimer.restart();
        } else {
            lastMoveFlashTimer.start();
        }
    }

    public void render(Board board, boolean boardInputEnabled) {
        ensureBoardSize(board.getSize());
        if (flashedCellIndex >= board.getCellCount()
                || (flashedCellIndex >= 0 && board.getCell(flashedCellIndex) == Player.EMPTY)) {
            clearLastMoveFlash();
        }

        Set<Integer> winningIndexes = new HashSet<>();
        for (int index : board.getWinningLine()) {
            winningIndexes.add(index);
        }

        updateWinningAnimation(winningIndexes);

        for (int i = 0; i < cells.size(); i++) {
            BoardCellButton button = cells.get(i);
            Player player = board.getCell(i);
            boolean winningCell = winningIndexes.contains(i);
            Font symbolFont = winningCell ? getWinningFont() : buttonFont;
            button.setSymbol(player, symbolFont, resolveSymbolColor(player, winningCell));
            button.setEnabled(boardInputEnabled && board.isMoveValid(i));
        }
        lastBoardInputEnabled = boardInputEnabled;
    }

    public void updateInputState(Board board, boolean boardInputEnabled) {
        ensureBoardSize(board.getSize());
        if (lastBoardInputEnabled != null && lastBoardInputEnabled == boardInputEnabled) {
            return;
        }

        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setEnabled(boardInputEnabled && board.isMoveValid(i));
        }
        lastBoardInputEnabled = boardInputEnabled;
    }

    private void ensureBoardSize(int size) {
        if (size == currentBoardSize) {
            return;
        }

        currentBoardSize = size;
        cells.clear();
        removeAll();
        clearLastMoveFlash();
        lastBoardInputEnabled = null;
        setLayout(new GridLayout(size, size, gridGap(size), gridGap(size)));

        buttonFont = new Font(Font.SANS_SERIF, Font.BOLD, baseFontSize(size));
        winningFont = buttonFont.deriveFont(Font.BOLD, buttonFont.getSize2D() + 4f);

        for (int i = 0; i < size * size; i++) {
            int index = i;
            BoardCellButton button = new BoardCellButton();
            button.setFont(buttonFont);
            button.setFocusPainted(false);
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.setVerticalAlignment(SwingConstants.CENTER);
            button.addActionListener(event -> {
                if (clickListener != null) {
                    clickListener.accept(index);
                }
            });
            cells.add(button);
            add(button);
        }

        revalidate();
        repaint();
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
        for (int i = 0; i < cells.size(); i++) {
            BoardCellButton button = cells.get(i);
            boolean winningCell = animatedWinningIndexes.contains(i);
            Player player = button.getPlayer();
            Font symbolFont = winningCell ? getWinningFont() : buttonFont;
            button.setSymbol(player, symbolFont, resolveSymbolColor(player, winningCell));
        }
        repaint();
    }

    private void advanceLastMoveFlash() {
        if (flashedCellIndex < 0 || flashedCellIndex >= cells.size()) {
            clearLastMoveFlash();
            return;
        }

        flashPhase = !flashPhase;
        flashStepsRemaining--;
        applyLastMoveFlashState();

        if (flashStepsRemaining <= 0) {
            clearLastMoveFlash();
        }
    }

    private void applyLastMoveFlashState() {
        for (int i = 0; i < cells.size(); i++) {
            BoardCellButton button = cells.get(i);
            if (i == flashedCellIndex) {
                button.setFlashOutlineColor(flashPhase ? LAST_MOVE_FLASH_PRIMARY : LAST_MOVE_FLASH_SECONDARY);
            } else {
                button.setFlashOutlineColor(null);
            }
        }
    }

    private void clearLastMoveFlash() {
        flashedCellIndex = -1;
        flashStepsRemaining = 0;
        flashPhase = false;
        if (lastMoveFlashTimer.isRunning()) {
            lastMoveFlashTimer.stop();
        }
        for (BoardCellButton button : cells) {
            button.setFlashOutlineColor(null);
        }
    }

    private Font getWinningFont() {
        return pulsePhase ? winningFont : buttonFont;
    }

    private Color resolveSymbolColor(Player player, boolean winningCell) {
        if (winningCell && player != Player.EMPTY) {
            return WINNING_SYMBOL_COLOR;
        }
        return switch (player) {
            case X -> isDarkTheme() ? X_DARK_THEME_COLOR : X_COLOR;
            case O -> O_COLOR;
            case EMPTY -> UIManager.getColor("Button.foreground") != null
                    ? UIManager.getColor("Button.foreground")
                    : Color.WHITE;
        };
    }

    private Color getDefaultButtonColor() {
        Color color = UIManager.getColor("Button.background");
        return color != null ? color : new BoardCellButton().getBackground();
    }

    private boolean isDarkTheme() {
        Color background = getDefaultButtonColor();
        int brightness = (background.getRed() + background.getGreen() + background.getBlue()) / 3;
        return brightness < 150;
    }

    private int gridGap(int size) {
        return size >= 10 ? 2 : size >= 7 ? 3 : size >= 5 ? 5 : 8;
    }

    private int baseFontSize(int size) {
        return switch (size) {
            case 3 -> 52;
            case 4 -> 44;
            case 5 -> 38;
            case 6 -> 33;
            case 7 -> 28;
            case 8 -> 24;
            case 9 -> 21;
            case 10 -> 18;
            default -> 24;
        };
    }

    private static final class BoardCellButton extends javax.swing.JButton {

        private Player player = Player.EMPTY;
        private Font symbolFont;
        private Color symbolColor;
        private Color flashOutlineColor;

        void setSymbol(Player player, Font symbolFont, Color symbolColor) {
            this.player = player;
            this.symbolFont = symbolFont;
            this.symbolColor = symbolColor;
            setText("");
            repaint();
        }

        Player getPlayer() {
            return player;
        }

        void setFlashOutlineColor(Color flashOutlineColor) {
            this.flashOutlineColor = flashOutlineColor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (player == Player.EMPTY || symbolFont == null || symbolColor == null) {
                paintFlashOverlay(graphics);
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(symbolFont);
                g2.setColor(symbolColor);

                String text = String.valueOf(player.getSymbol());
                FontMetrics metrics = g2.getFontMetrics();
                int textWidth = metrics.stringWidth(text);
                int textX = (getWidth() - textWidth) / 2;
                int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                g2.drawString(text, textX, textY);
            } finally {
                g2.dispose();
            }

            paintFlashOverlay(graphics);
        }

        private void paintFlashOverlay(Graphics graphics) {
            if (flashOutlineColor == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(flashOutlineColor);
                g2.setStroke(new BasicStroke(4f));
                g2.drawRoundRect(4, 4, getWidth() - 9, getHeight() - 9, 18, 18);

                g2.setColor(new Color(
                        flashOutlineColor.getRed(),
                        flashOutlineColor.getGreen(),
                        flashOutlineColor.getBlue(),
                        Math.max(80, flashOutlineColor.getAlpha() / 2)));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(8, 8, getWidth() - 17, getHeight() - 17, 14, 14);
            } finally {
                g2.dispose();
            }
        }
    }
}
