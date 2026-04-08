package com.ai.tictactoe.ui;

import com.ai.tictactoe.controller.GameController;
import com.ai.tictactoe.controller.GameSnapshot;
import com.ai.tictactoe.controller.GameView;
import com.ai.tictactoe.model.GameMode;
import com.ai.tictactoe.model.StartMode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

public final class MainFrame extends JFrame implements GameView {

    private final BoardPanel boardPanel;
    private final JComboBox<GameMode> modeSelector;
    private final JComboBox<StartMode> startModeSelector;
    private final JSlider simulationSlider;
    private final JLabel simulationLabel;
    private final JLabel statusLabel;
    private final JLabel humanScoreLabel;
    private final JLabel computerScoreLabel;
    private final JLabel drawScoreLabel;
    private final JButton newGameButton;
    private final JButton resetScoreButton;
    private final JToggleButton darkModeToggle;
    private final GameController controller;

    private GameSnapshot lastSnapshot;
    private boolean darkMode;
    private boolean updatingUi;

    public MainFrame() {
        super("AI Tic-Tac-Toe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        boardPanel = new BoardPanel();
        modeSelector = new JComboBox<>(GameMode.values());
        startModeSelector = new JComboBox<>(StartMode.values());
        simulationSlider = new JSlider(
                GameController.MIN_MCTS_SIMULATIONS,
                GameController.MAX_MCTS_SIMULATIONS,
                GameController.DEFAULT_MCTS_SIMULATIONS);
        simulationLabel = new JLabel();
        statusLabel = new JLabel(" ");
        humanScoreLabel = new JLabel();
        computerScoreLabel = new JLabel();
        drawScoreLabel = new JLabel();
        newGameButton = new JButton("New Game");
        resetScoreButton = new JButton("Reset Score");
        darkModeToggle = new JToggleButton("Dark UI");

        configureWindow();
        configureControls();
        buildLayout();

        controller = new GameController(this, new SwingAiMoveExecutor());
        wireEvents();
    }

    private void configureWindow() {
        setLayout(new BorderLayout(12, 12));
        getRootPane().setBorder(new EmptyBorder(12, 12, 12, 12));
        setMinimumSize(new Dimension(540, 660));
        setLocationByPlatform(true);
    }

    private void configureControls() {
        simulationSlider.setMajorTickSpacing(2_000);
        simulationSlider.setMinorTickSpacing(500);
        simulationSlider.setPaintTicks(true);
        simulationSlider.setPaintLabels(false);

        Font statusFont = statusLabel.getFont().deriveFont(Font.BOLD, 14f);
        statusLabel.setFont(statusFont);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        darkModeToggle.setFocusPainted(false);
    }

    private void buildLayout() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(buildModePanel());
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(buildSliderPanel());
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(buildActionsPanel());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        bottomPanel.add(buildScorePanel(), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildModePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.add(new JLabel("Mode:"));
        panel.add(modeSelector);
        panel.add(new JLabel("Starts:"));
        panel.add(startModeSelector);
        return panel;
    }

    private JPanel buildSliderPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.add(simulationLabel, BorderLayout.WEST);
        panel.add(simulationSlider, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.add(newGameButton);
        panel.add(resetScoreButton);
        panel.add(darkModeToggle);
        return panel;
    }

    private JPanel buildScorePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.add(humanScoreLabel);
        panel.add(computerScoreLabel);
        panel.add(drawScoreLabel);
        return panel;
    }

    private void wireEvents() {
        boardPanel.setClickListener(index -> controller.handleHumanMove(index));
        newGameButton.addActionListener(event -> controller.startNewGame());
        resetScoreButton.addActionListener(event -> controller.resetScore());
        modeSelector.addActionListener(event -> {
            if (!updatingUi) {
                controller.changeMode((GameMode) modeSelector.getSelectedItem());
            }
        });
        startModeSelector.addActionListener(event -> {
            if (!updatingUi) {
                controller.changeStartMode((StartMode) startModeSelector.getSelectedItem());
            }
        });
        simulationSlider.addChangeListener(event -> {
            if (!updatingUi) {
                controller.changeMctsSimulationCount(simulationSlider.getValue());
            }
        });
        darkModeToggle.addActionListener(event -> {
            darkMode = darkModeToggle.isSelected();
            ThemeManager.applyTheme(darkMode, this);
            updateThemeToggleText();
            repaintCurrentBoard();
        });
    }

    @Override
    public void render(GameSnapshot snapshot) {
        lastSnapshot = snapshot;
        updatingUi = true;
        modeSelector.setSelectedItem(snapshot.gameMode());
        startModeSelector.setSelectedItem(snapshot.startMode());
        simulationSlider.setValue(snapshot.mctsSimulationCount());
        simulationLabel.setText("AI Simulations: " + snapshot.mctsSimulationCount());
        simulationLabel.setVisible(snapshot.gameMode().usesSimulationSlider());
        simulationSlider.setVisible(snapshot.gameMode().usesSimulationSlider());
        simulationSlider.setEnabled(snapshot.gameMode().usesSimulationSlider() && !snapshot.aiThinking());

        boardPanel.render(snapshot.board(), snapshot.boardInputEnabled());
        humanScoreLabel.setText("Human: " + snapshot.scoreBoard().humanWins());
        computerScoreLabel.setText("Computer: " + snapshot.scoreBoard().computerWins());
        drawScoreLabel.setText("Draws: " + snapshot.scoreBoard().draws());
        statusLabel.setText(snapshot.statusMessage());
        updatingUi = false;
    }

    private void repaintCurrentBoard() {
        if (lastSnapshot != null) {
            render(lastSnapshot);
        }
    }

    private void updateThemeToggleText() {
        darkModeToggle.setText(darkMode ? "Light UI" : "Dark UI");
    }
}
