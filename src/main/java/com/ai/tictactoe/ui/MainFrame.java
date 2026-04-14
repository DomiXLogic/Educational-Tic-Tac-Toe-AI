package com.ai.tictactoe.ui;

import com.ai.tictactoe.controller.GameController;
import com.ai.tictactoe.controller.GameSnapshot;
import com.ai.tictactoe.controller.GameView;
import com.ai.tictactoe.monitor.AlgorithmMonitorFrame;
import com.ai.tictactoe.model.ArtificialStupidityLevel;
import com.ai.tictactoe.model.BoardSize;
import com.ai.tictactoe.model.BotStrategyType;
import com.ai.tictactoe.model.GameMode;
import com.ai.tictactoe.model.Player;
import com.ai.tictactoe.model.StartMode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public final class MainFrame extends JFrame implements GameView {

    private final BoardPanel boardPanel;
    private final JComboBox<BoardSize> boardSizeSelector;
    private final JComboBox<GameMode> modeSelector;
    private final JComboBox<StartMode> startModeSelector;
    private final JComboBox<BotStrategyType> xStrategySelector;
    private final JComboBox<BotStrategyType> oStrategySelector;
    private final JSlider simulationSlider;
    private final JSlider stupiditySlider;
    private final JSlider xSimulationSlider;
    private final JSlider xStupiditySlider;
    private final JSlider oSimulationSlider;
    private final JSlider oStupiditySlider;
    private final JSlider moveSpeedSlider;
    private final JLabel simulationLabel;
    private final JLabel stupidityLabel;
    private final JLabel xSimulationLabel;
    private final JLabel xStupidityLabel;
    private final JLabel oSimulationLabel;
    private final JLabel oStupidityLabel;
    private final JLabel moveSpeedLabel;
    private final JLabel statusLabel;
    private final JLabel humanScoreLabel;
    private final JLabel computerScoreLabel;
    private final JLabel drawScoreLabel;
    private final JLabel winConditionLabel;
    private final JLabel startModeLabel;
    private final JCheckBox mctsArtificialStupidityCheckBox;
    private final JCheckBox xArtificialStupidityCheckBox;
    private final JCheckBox oArtificialStupidityCheckBox;
    private final JButton newGameButton;
    private final JButton resetScoreButton;
    private final JButton showAiMoveButton;
    private final JButton startPcVsPcButton;
    private final JButton pausePcVsPcButton;
    private final JCheckBoxMenuItem darkModeMenuItem;
    private final JPanel standardControlsPanel;
    private final JPanel pcVsPcConfigPanel;
    private final JPanel pcVsPcActionsPanel;
    private final AlgorithmMonitorFrame monitorFrame;
    private final GameController controller;

    private GameSnapshot lastSnapshot;
    private String lastBoardSignature;
    private Boolean lastBoardInputEnabled;
    private boolean darkMode;
    private boolean updatingUi;

    public MainFrame() {
        super("AI Tic-Tac-Toe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        boardPanel = new BoardPanel();
        boardSizeSelector = new JComboBox<>(BoardSize.values());
        modeSelector = new JComboBox<>(GameMode.values());
        startModeSelector = new JComboBox<>(StartMode.values());
        xStrategySelector = new JComboBox<>(BotStrategyType.values());
        oStrategySelector = new JComboBox<>(BotStrategyType.values());
        simulationSlider = createSimulationSlider();
        stupiditySlider = createStupiditySlider();
        xSimulationSlider = createSimulationSlider();
        xStupiditySlider = createStupiditySlider();
        oSimulationSlider = createSimulationSlider();
        oStupiditySlider = createStupiditySlider();
        moveSpeedSlider = new JSlider(
                GameController.MIN_PC_VS_PC_DELAY_MS,
                GameController.MAX_PC_VS_PC_DELAY_MS,
                GameController.DEFAULT_PC_VS_PC_DELAY_MS);
        simulationLabel = new JLabel();
        stupidityLabel = new JLabel();
        xSimulationLabel = new JLabel();
        xStupidityLabel = new JLabel();
        oSimulationLabel = new JLabel();
        oStupidityLabel = new JLabel();
        moveSpeedLabel = new JLabel();
        statusLabel = new JLabel(" ");
        humanScoreLabel = new JLabel();
        computerScoreLabel = new JLabel();
        drawScoreLabel = new JLabel();
        winConditionLabel = new JLabel();
        startModeLabel = new JLabel("Starts:");
        mctsArtificialStupidityCheckBox = new JCheckBox("Enable Artificial Stupidity");
        xArtificialStupidityCheckBox = new JCheckBox("Enable Artificial Stupidity");
        oArtificialStupidityCheckBox = new JCheckBox("Enable Artificial Stupidity");
        newGameButton = new JButton("New Game");
        resetScoreButton = new JButton("Reset Score");
        showAiMoveButton = new JButton("Show AI Move");
        startPcVsPcButton = new JButton("Start");
        pausePcVsPcButton = new JButton("Pause");
        darkModeMenuItem = new JCheckBoxMenuItem("Dark UI");
        standardControlsPanel = new JPanel();
        pcVsPcConfigPanel = new JPanel();
        pcVsPcActionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        monitorFrame = new AlgorithmMonitorFrame();
        lastBoardSignature = null;
        lastBoardInputEnabled = null;

        configureWindow();
        configureControls();
        setJMenuBar(buildMenuBar());
        buildLayout();

        controller = new GameController(
                this,
                new SwingAiMoveExecutor(),
                new SwingAutoMoveScheduler(),
                monitorFrame);
        wireEvents();
    }

    private void configureWindow() {
        setLayout(new BorderLayout(12, 12));
        getRootPane().setBorder(new EmptyBorder(12, 12, 12, 12));
        setMinimumSize(new Dimension(720, 780));
        setLocationByPlatform(true);
    }

    private void configureControls() {
        moveSpeedSlider.setMajorTickSpacing(1_000);
        moveSpeedSlider.setMinorTickSpacing(200);
        moveSpeedSlider.setPaintTicks(true);
        moveSpeedSlider.setPaintLabels(false);

        Font statusFont = statusLabel.getFont().deriveFont(Font.BOLD, 14f);
        statusLabel.setFont(statusFont);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        winConditionLabel.setFont(winConditionLabel.getFont().deriveFont(Font.BOLD, 12f));

        String stupidityHint = "Lower stupidity means stronger, more deterministic MCTS.";
        stupiditySlider.setToolTipText(stupidityHint);
        xStupiditySlider.setToolTipText(stupidityHint);
        oStupiditySlider.setToolTipText(stupidityHint);
        stupidityLabel.setToolTipText(stupidityHint);
        xStupidityLabel.setToolTipText(stupidityHint);
        oStupidityLabel.setToolTipText(stupidityHint);
        mctsArtificialStupidityCheckBox.setToolTipText("Turn controlled imperfection on or off for the main MCTS opponent.");
        xArtificialStupidityCheckBox.setToolTipText("Turn controlled imperfection on or off for the X bot.");
        oArtificialStupidityCheckBox.setToolTipText("Turn controlled imperfection on or off for the O bot.");
    }

    private JSlider createSimulationSlider() {
        JSlider slider = new JSlider(
                GameController.MIN_MCTS_SIMULATIONS,
                GameController.MAX_MCTS_SIMULATIONS,
                GameController.DEFAULT_MCTS_SIMULATIONS);
        slider.setMajorTickSpacing(2_000);
        slider.setMinorTickSpacing(500);
        slider.setPaintTicks(true);
        slider.setPaintLabels(false);
        return slider;
    }

    private JSlider createStupiditySlider() {
        JSlider slider = new JSlider(
                ArtificialStupidityLevel.SUPER_LOW.getSliderValue(),
                ArtificialStupidityLevel.EXTRA_HIGH.getSliderValue(),
                ArtificialStupidityLevel.EXTRA_HIGH.getSliderValue());
        slider.setMajorTickSpacing(1);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(false);
        slider.setSnapToTicks(true);
        return slider;
    }

    private void buildLayout() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(buildModePanel());
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(buildStandardControlsPanel());
        topPanel.add(buildPcVsPcConfigPanel());
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(buildPcVsPcActionsPanel());
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
        panel.add(new JLabel("Goal:"));
        panel.add(boardSizeSelector);
        panel.add(startModeLabel);
        panel.add(startModeSelector);
        return panel;
    }

    private JPanel buildStandardControlsPanel() {
        standardControlsPanel.setLayout(new BoxLayout(standardControlsPanel, BoxLayout.Y_AXIS));
        standardControlsPanel.setBorder(BorderFactory.createTitledBorder("MCTS Controls"));
        standardControlsPanel.add(buildLabeledSliderRow(simulationLabel, simulationSlider));
        standardControlsPanel.add(Box.createVerticalStrut(6));
        standardControlsPanel.add(buildCheckBoxRow(mctsArtificialStupidityCheckBox));
        standardControlsPanel.add(Box.createVerticalStrut(6));
        standardControlsPanel.add(buildLabeledSliderRow(stupidityLabel, stupiditySlider));
        return standardControlsPanel;
    }

    private JPanel buildPcVsPcConfigPanel() {
        pcVsPcConfigPanel.setLayout(new BoxLayout(pcVsPcConfigPanel, BoxLayout.Y_AXIS));
        pcVsPcConfigPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JPanel botsContainer = new JPanel(new GridLayout(1, 2, 14, 0));
        botsContainer.add(buildBotPanel("X Bot", xStrategySelector, xSimulationLabel, xSimulationSlider, xStupidityLabel, xStupiditySlider));
        botsContainer.add(buildBotPanel("O Bot", oStrategySelector, oSimulationLabel, oSimulationSlider, oStupidityLabel, oStupiditySlider));
        botsContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));

        JPanel speedPanel = new JPanel(new BorderLayout(8, 0));
        speedPanel.setBorder(BorderFactory.createTitledBorder("Playback"));
        speedPanel.add(moveSpeedLabel, BorderLayout.WEST);
        speedPanel.add(moveSpeedSlider, BorderLayout.CENTER);
        speedPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        pcVsPcConfigPanel.add(botsContainer);
        pcVsPcConfigPanel.add(Box.createVerticalStrut(8));
        pcVsPcConfigPanel.add(speedPanel);
        return pcVsPcConfigPanel;
    }

    private JPanel buildBotPanel(String title,
                                 JComboBox<BotStrategyType> strategySelector,
                                 JLabel simulationTextLabel,
                                 JSlider simulationBudgetSlider,
                                 JLabel stupidityTextLabel,
                                 JSlider stupidityBudgetSlider) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JPanel strategyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        strategyRow.add(new JLabel("Strategy:"));
        strategyRow.add(strategySelector);

        panel.add(strategyRow);
        panel.add(Box.createVerticalStrut(8));
        panel.add(buildLabeledSliderRow(simulationTextLabel, simulationBudgetSlider));
        panel.add(Box.createVerticalStrut(6));
        panel.add(buildCheckBoxRow(title.startsWith("X") ? xArtificialStupidityCheckBox : oArtificialStupidityCheckBox));
        panel.add(Box.createVerticalStrut(6));
        panel.add(buildLabeledSliderRow(stupidityTextLabel, stupidityBudgetSlider));
        return panel;
    }

    private JPanel buildLabeledSliderRow(JLabel textLabel, JSlider slider) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.add(textLabel, BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildCheckBoxRow(JCheckBox checkBox) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.add(checkBox);
        return row;
    }

    private JPanel buildPcVsPcActionsPanel() {
        pcVsPcActionsPanel.add(startPcVsPcButton);
        pcVsPcActionsPanel.add(pausePcVsPcButton);
        return pcVsPcActionsPanel;
    }

    private JPanel buildActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.add(newGameButton);
        panel.add(resetScoreButton);
        panel.add(showAiMoveButton);
        panel.add(winConditionLabel);
        return panel;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(event -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        fileMenu.add(closeItem);

        JMenu viewMenu = new JMenu("View");
        JMenuItem algorithmMonitorItem = new JMenuItem("Algorithm Monitor");
        algorithmMonitorItem.addActionListener(event -> monitorFrame.setVisible(true));
        darkModeMenuItem.addActionListener(event -> {
            darkMode = darkModeMenuItem.isSelected();
            applyThemeSelection();
        });
        viewMenu.add(algorithmMonitorItem);
        viewMenu.add(darkModeMenuItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(event -> showAboutDialog());
        JMenuItem metricsDocItem = new JMenuItem("Metrics Doc");
        metricsDocItem.addActionListener(event -> showMetricsDocDialog());
        helpMenu.add(aboutItem);
        helpMenu.add(metricsDocItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        return menuBar;
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
        showAiMoveButton.addActionListener(event -> {
            if (lastSnapshot != null && lastSnapshot.lastComputerMove() >= 0) {
                boardPanel.flashCell(lastSnapshot.lastComputerMove());
            }
        });
        startPcVsPcButton.addActionListener(event -> controller.startPcVsPc());
        pausePcVsPcButton.addActionListener(event -> controller.pausePcVsPc());
        modeSelector.addActionListener(event -> {
            if (!updatingUi) {
                controller.changeMode((GameMode) modeSelector.getSelectedItem());
            }
        });
        boardSizeSelector.addActionListener(event -> {
            if (!updatingUi) {
                controller.changeBoardSize((BoardSize) boardSizeSelector.getSelectedItem());
            }
        });
        startModeSelector.addActionListener(event -> {
            if (!updatingUi) {
                controller.changeStartMode((StartMode) startModeSelector.getSelectedItem());
            }
        });
        simulationSlider.addChangeListener(event -> {
            if (!updatingUi) {
                simulationLabel.setText("AI Simulations: " + simulationSlider.getValue());
                if (!simulationSlider.getValueIsAdjusting()) {
                    controller.changeMctsSimulationCount(simulationSlider.getValue());
                }
            }
        });
        mctsArtificialStupidityCheckBox.addActionListener(event -> {
            if (!updatingUi) {
                controller.changeMctsArtificialStupidityEnabled(mctsArtificialStupidityCheckBox.isSelected());
            }
        });
        stupiditySlider.addChangeListener(event -> {
            if (!updatingUi) {
                ArtificialStupidityLevel level = ArtificialStupidityLevel.fromSliderValue(stupiditySlider.getValue());
                stupidityLabel.setText("Artificial Stupidity: " + level.getDisplayName());
                if (!stupiditySlider.getValueIsAdjusting()) {
                    controller.changeMctsStupidityLevel(level);
                }
            }
        });
        xStrategySelector.addActionListener(event -> {
            if (!updatingUi) {
                controller.changePcVsPcStrategy(Player.X, (BotStrategyType) xStrategySelector.getSelectedItem());
            }
        });
        oStrategySelector.addActionListener(event -> {
            if (!updatingUi) {
                controller.changePcVsPcStrategy(Player.O, (BotStrategyType) oStrategySelector.getSelectedItem());
            }
        });
        xSimulationSlider.addChangeListener(event -> {
            if (!updatingUi) {
                xSimulationLabel.setText("X MCTS: " + xSimulationSlider.getValue());
                if (!xSimulationSlider.getValueIsAdjusting()) {
                    controller.changePcVsPcSimulationCount(Player.X, xSimulationSlider.getValue());
                }
            }
        });
        xArtificialStupidityCheckBox.addActionListener(event -> {
            if (!updatingUi) {
                controller.changePcVsPcArtificialStupidityEnabled(Player.X, xArtificialStupidityCheckBox.isSelected());
            }
        });
        xStupiditySlider.addChangeListener(event -> {
            if (!updatingUi) {
                ArtificialStupidityLevel level = ArtificialStupidityLevel.fromSliderValue(xStupiditySlider.getValue());
                xStupidityLabel.setText("X Stupidity: " + level.getDisplayName());
                if (!xStupiditySlider.getValueIsAdjusting()) {
                    controller.changePcVsPcStupidityLevel(Player.X, level);
                }
            }
        });
        oSimulationSlider.addChangeListener(event -> {
            if (!updatingUi) {
                oSimulationLabel.setText("O MCTS: " + oSimulationSlider.getValue());
                if (!oSimulationSlider.getValueIsAdjusting()) {
                    controller.changePcVsPcSimulationCount(Player.O, oSimulationSlider.getValue());
                }
            }
        });
        oArtificialStupidityCheckBox.addActionListener(event -> {
            if (!updatingUi) {
                controller.changePcVsPcArtificialStupidityEnabled(Player.O, oArtificialStupidityCheckBox.isSelected());
            }
        });
        oStupiditySlider.addChangeListener(event -> {
            if (!updatingUi) {
                ArtificialStupidityLevel level = ArtificialStupidityLevel.fromSliderValue(oStupiditySlider.getValue());
                oStupidityLabel.setText("O Stupidity: " + level.getDisplayName());
                if (!oStupiditySlider.getValueIsAdjusting()) {
                    controller.changePcVsPcStupidityLevel(Player.O, level);
                }
            }
        });
        moveSpeedSlider.addChangeListener(event -> {
            if (!updatingUi) {
                moveSpeedLabel.setText("Move Speed: " + formatMoveSpeed(moveSpeedSlider.getValue()));
                if (!moveSpeedSlider.getValueIsAdjusting()) {
                    controller.changePcVsPcMoveDelay(moveSpeedSlider.getValue());
                }
            }
        });
    }

    @Override
    public void render(GameSnapshot snapshot) {
        lastSnapshot = snapshot;
        monitorFrame.updateSnapshot(snapshot);
        updatingUi = true;

        modeSelector.setSelectedItem(snapshot.gameMode());
        boardSizeSelector.setSelectedItem(snapshot.boardSize());
        startModeSelector.setSelectedItem(snapshot.startMode());
        simulationSlider.setValue(snapshot.mctsSimulationCount());
        mctsArtificialStupidityCheckBox.setSelected(snapshot.mctsArtificialStupidityEnabled());
        stupiditySlider.setValue(snapshot.mctsStupidityLevel().getSliderValue());
        simulationLabel.setText("AI Simulations: " + snapshot.mctsSimulationCount());
        stupidityLabel.setText(buildStupidityLabel(snapshot.mctsArtificialStupidityEnabled(), snapshot.mctsStupidityLevel()));

        xStrategySelector.setSelectedItem(snapshot.xBotStrategy());
        oStrategySelector.setSelectedItem(snapshot.oBotStrategy());
        xSimulationSlider.setValue(snapshot.xMctsSimulationCount());
        xArtificialStupidityCheckBox.setSelected(snapshot.xMctsArtificialStupidityEnabled());
        xStupiditySlider.setValue(snapshot.xMctsStupidityLevel().getSliderValue());
        oSimulationSlider.setValue(snapshot.oMctsSimulationCount());
        oArtificialStupidityCheckBox.setSelected(snapshot.oMctsArtificialStupidityEnabled());
        oStupiditySlider.setValue(snapshot.oMctsStupidityLevel().getSliderValue());
        moveSpeedSlider.setValue(snapshot.pcVsPcMoveDelayMillis());
        xSimulationLabel.setText(buildBotSimulationLabel("X", snapshot.xBotStrategy(), snapshot.xMctsSimulationCount()));
        xStupidityLabel.setText(buildBotStupidityLabel("X", snapshot.xBotStrategy(),
                snapshot.xMctsArtificialStupidityEnabled(), snapshot.xMctsStupidityLevel()));
        oSimulationLabel.setText(buildBotSimulationLabel("O", snapshot.oBotStrategy(), snapshot.oMctsSimulationCount()));
        oStupidityLabel.setText(buildBotStupidityLabel("O", snapshot.oBotStrategy(),
                snapshot.oMctsArtificialStupidityEnabled(), snapshot.oMctsStupidityLevel()));
        moveSpeedLabel.setText("Move Speed: " + formatMoveSpeed(snapshot.pcVsPcMoveDelayMillis()));

        boolean pcVsPc = snapshot.gameMode().isPcVsPc();
        startModeLabel.setVisible(!pcVsPc);
        startModeSelector.setVisible(!pcVsPc);
        standardControlsPanel.setVisible(!pcVsPc);
        pcVsPcConfigPanel.setVisible(pcVsPc);
        pcVsPcActionsPanel.setVisible(pcVsPc);
        simulationLabel.setVisible(snapshot.gameMode().usesSimulationSlider());
        simulationSlider.setVisible(snapshot.gameMode().usesSimulationSlider());
        mctsArtificialStupidityCheckBox.setVisible(snapshot.gameMode().usesSimulationSlider());
        stupidityLabel.setVisible(snapshot.gameMode().usesSimulationSlider());
        stupiditySlider.setVisible(snapshot.gameMode().usesSimulationSlider());
        simulationSlider.setEnabled(snapshot.gameMode().usesSimulationSlider() && !snapshot.aiThinking());
        mctsArtificialStupidityCheckBox.setEnabled(snapshot.gameMode().usesSimulationSlider() && !snapshot.aiThinking());
        stupiditySlider.setEnabled(snapshot.gameMode().usesSimulationSlider()
                && !snapshot.aiThinking()
                && snapshot.mctsArtificialStupidityEnabled());

        boolean pcVsPcEditable = pcVsPc && !snapshot.pcVsPcRunning() && !snapshot.aiThinking() && !snapshot.gameOver();
        xStrategySelector.setEnabled(pcVsPcEditable);
        oStrategySelector.setEnabled(pcVsPcEditable);
        xSimulationSlider.setEnabled(pcVsPcEditable && snapshot.xBotStrategy() == BotStrategyType.MCTS);
        xArtificialStupidityCheckBox.setEnabled(pcVsPcEditable && snapshot.xBotStrategy() == BotStrategyType.MCTS);
        xStupiditySlider.setEnabled(pcVsPcEditable
                && snapshot.xBotStrategy() == BotStrategyType.MCTS
                && snapshot.xMctsArtificialStupidityEnabled());
        oSimulationSlider.setEnabled(pcVsPcEditable && snapshot.oBotStrategy() == BotStrategyType.MCTS);
        oArtificialStupidityCheckBox.setEnabled(pcVsPcEditable && snapshot.oBotStrategy() == BotStrategyType.MCTS);
        oStupiditySlider.setEnabled(pcVsPcEditable
                && snapshot.oBotStrategy() == BotStrategyType.MCTS
                && snapshot.oMctsArtificialStupidityEnabled());
        moveSpeedSlider.setEnabled(pcVsPc);
        startPcVsPcButton.setEnabled(pcVsPc && !snapshot.pcVsPcRunning() && !snapshot.aiThinking() && !snapshot.gameOver());
        pausePcVsPcButton.setEnabled(pcVsPc && (snapshot.pcVsPcRunning() || snapshot.aiThinking()) && !snapshot.gameOver());

        winConditionLabel.setText(buildWinConditionText(snapshot.boardSize()));
        showAiMoveButton.setEnabled(snapshot.lastComputerMove() >= 0 && !snapshot.aiThinking());

        String boardSignature = buildBoardSignature(snapshot);
        if (!boardSignature.equals(lastBoardSignature)) {
            boardPanel.render(snapshot.board(), snapshot.boardInputEnabled());
            lastBoardSignature = boardSignature;
            lastBoardInputEnabled = snapshot.boardInputEnabled();
        } else if (lastBoardInputEnabled == null || lastBoardInputEnabled != snapshot.boardInputEnabled()) {
            boardPanel.updateInputState(snapshot.board(), snapshot.boardInputEnabled());
            lastBoardInputEnabled = snapshot.boardInputEnabled();
        }

        if (pcVsPc) {
            humanScoreLabel.setText("X Wins: " + snapshot.scoreBoard().humanWins());
            computerScoreLabel.setText("O Wins: " + snapshot.scoreBoard().computerWins());
        } else {
            humanScoreLabel.setText("Human: " + snapshot.scoreBoard().humanWins());
            computerScoreLabel.setText("Computer: " + snapshot.scoreBoard().computerWins());
        }
        drawScoreLabel.setText("Draws: " + snapshot.scoreBoard().draws());
        statusLabel.setText(snapshot.statusMessage());
        updatingUi = false;
    }

    private void repaintCurrentBoard() {
        if (lastSnapshot != null) {
            lastBoardSignature = null;
            lastBoardInputEnabled = null;
            render(lastSnapshot);
        }
    }

    private void applyThemeSelection() {
        ThemeManager.applyTheme(darkMode, this);
        SwingUtilities.updateComponentTreeUI(monitorFrame);
        monitorFrame.pack();
        repaintCurrentBoard();
    }

    private String buildWinConditionText(BoardSize boardSize) {
        return "Need "
                + boardSize.getWinLength()
                + " in a row to win"
                + (boardSize.getBoardDimension() != boardSize.getWinLength()
                ? " on " + boardSize.getBoardDimension() + "x" + boardSize.getBoardDimension()
                : "");
    }

    private String formatMoveSpeed(int delayMillis) {
        return String.format("%.1f sec/move", delayMillis / 1000.0);
    }

    private String buildBotStupidityLabel(String botName,
                                          BotStrategyType strategyType,
                                          boolean enabled,
                                          ArtificialStupidityLevel level) {
        if (strategyType != BotStrategyType.MCTS) {
            return botName + " Stupidity: N/A (Algo)";
        }
        return botName + " Stupidity: " + (enabled ? level.getDisplayName() : "Off");
    }

    private String buildBotSimulationLabel(String botName, BotStrategyType strategyType, int simulations) {
        if (strategyType != BotStrategyType.MCTS) {
            return botName + " MCTS: N/A (Algo)";
        }
        return botName + " MCTS: " + simulations;
    }

    private String buildStupidityLabel(boolean enabled, ArtificialStupidityLevel level) {
        return "Artificial Stupidity: " + (enabled ? level.getDisplayName() : "Off");
    }

    private void showAboutDialog() {
        String html = """
                <html>
                <body style='font-family:sans-serif;padding:12px;'>
                <h2>Educational Tic-Tac-Toe AI</h2>
                <p>Developed by <b>Katsaras Kyriakos</b>.</p>
                <p>
                Website:
                <a href='https://kkatsaras.gr'>kkatsaras.gr</a><br/>
                GitHub Repository:
                <a href='https://github.com/DomiXLogic/Educational-Tic-Tac-Toe-AI'>Educational-Tic-Tac-Toe-AI</a>
                </p>
                <p>
                This project is a classroom-oriented Java Swing application for studying
                deterministic search, Monte Carlo Tree Search, controlled imperfection,
                runtime telemetry, and automated algorithm-versus-algorithm observation.
                </p>
                </body>
                </html>
                """;
        HtmlContentDialog dialog = new HtmlContentDialog(this, "About", html, 560, 300);
        dialog.setVisible(true);
    }

    private void showMetricsDocDialog() {
        String html = """
                <html>
                <body style='font-family:sans-serif;padding:12px;'>
                <h2>Metrics Documentation</h2>

                <h3>Current Game</h3>
                <ul>
                  <li><b>Mode</b>: The active gameplay profile, such as human play or PC vs PC.</li>
                  <li><b>Board</b>: The actual board dimension currently in play, such as 3x3 or 10x10.</li>
                  <li><b>Goal</b>: How many symbols in a row are required to win.</li>
                  <li><b>Turn</b>: Which side is currently expected to act, or whether the game has ended.</li>
                  <li><b>Starts</b>: The configured starter policy for human-play modes.</li>
                  <li><b>Simulations</b>: The current MCTS simulation budget from the main UI slider.</li>
                  <li><b>Stupidity</b>: The current Artificial Stupidity profile. If Artificial Stupidity is disabled, this appears as Off and MCTS runs without the teaching-oriented behavior shaping layer.</li>
                </ul>

                <h3>System Metrics</h3>
                <ul>
                  <li><b>CPU</b>: Estimated CPU load of this Java process at sampling time.</li>
                  <li><b>Heap</b>: Current used heap memory versus maximum available heap.</li>
                  <li><b>Threads</b>: Number of live threads in the running JVM.</li>
                  <li><b>GC</b>: Total garbage collection count and total collection time in milliseconds.</li>
                  <li><b>Process CPU % chart</b>: Recent CPU history for this process, similar in spirit to a task-manager trend chart.</li>
                  <li><b>Heap Used chart</b>: Recent heap memory usage history in megabytes.</li>
                </ul>

                <h3>Current Move Analytics</h3>
                <ul>
                  <li><b>Algorithm</b>: Which search strategy produced the current AI move.</li>
                  <li><b>Response</b>: Total wall-clock time from AI think start to selected move result.</li>
                  <li><b>Chosen Move</b>: The selected move in row, column, and raw move-index form.</li>
                  <li><b>Legal Moves</b>: Number of legal options available when the AI made its decision.</li>
                  <li><b>Phase</b>: Opening, Midgame, or Endgame estimate based on board occupancy.</li>
                  <li><b>Timestamp</b>: When the selected AI move finished.</li>
                  <li><b>CPU Delta</b>: Approximate process CPU consumption during that single think cycle.</li>
                  <li><b>Heap Delta</b>: Approximate heap memory change during that single think cycle.</li>
                </ul>

                <h3>Algorithm Internals</h3>
                <ul>
                  <li><b>Nodes / Steps</b>: Search effort indicator. For Minimax this is explored nodes; for MCTS it is a combined search-step estimate.</li>
                  <li><b>Alpha-Beta Cutoffs</b>: Number of pruning events triggered by alpha-beta search in Minimax.</li>
                  <li><b>Max Depth</b>: Deepest recursion level reached by Minimax.</li>
                  <li><b>Heuristic Score</b>: Evaluation score of the chosen move or candidate from the algorithm's internal evaluator.</li>
                  <li><b>Simulations</b>: Total Monte Carlo simulations completed by MCTS for the move.</li>
                  <li><b>Simulations/sec</b>: Approximate throughput of completed MCTS simulations per second.</li>
                  <li><b>Rollouts</b>: Average rollout length in moves and average rollout duration in milliseconds.</li>
                  <li><b>Confidence</b>: Best-child visit ratio in MCTS, used as an indicator of how dominant the selected move was among alternatives.</li>
                </ul>

                <h3>Artificial Stupidity</h3>
                <ul>
                  <li><b>Purpose</b>: Separates search budget from behavior shaping, so students can study how much MCTS computed versus how strictly it follows the strongest-looking candidate.</li>
                  <li><b>Enabled checkbox</b>: Turns the Artificial Stupidity layer on or off. When off, MCTS uses its strongest search profile in this project.</li>
                  <li><b>Super Low</b>: Almost no additional imperfection beyond the tactical guardrails.</li>
                  <li><b>Low</b>: Slightly softer final move selection.</li>
                  <li><b>Medium</b>: Balanced visible imperfection for classroom comparison.</li>
                  <li><b>High</b>: Noticeably less deterministic final move choice.</li>
                  <li><b>Extra High</b>: Maximum controlled imperfection, closest to the original tuned behavior.</li>
                  <li><b>Tactical guardrails</b>: Immediate winning moves are still taken, and immediate losing replies are avoided when a safe alternative exists.</li>
                </ul>

                <h3>Candidate Moves Table</h3>
                <ul>
                  <li><b>Move</b>: Raw board index of the candidate move.</li>
                  <li><b>Row / Col</b>: Human-readable candidate position.</li>
                  <li><b>Visits</b>: Number of visits for the candidate in MCTS. Minimax candidates may not use this field.</li>
                  <li><b>Avg Score</b>: Average reward observed for the candidate in MCTS.</li>
                  <li><b>Eval</b>: Evaluation score used to rank the move. For Minimax it is heuristic/search value; for MCTS it mirrors candidate quality.</li>
                </ul>

                <h3>Move History</h3>
                <ul>
                  <li><b>Game</b>: Internal game session id.</li>
                  <li><b>Move #</b>: Board progression step at which the AI move happened.</li>
                  <li><b>Algorithm</b>: Concrete algorithm profile used for that move.</li>
                  <li><b>Response (ms)</b>: Time to compute the move.</li>
                  <li><b>CPU Delta</b>: Approximate CPU usage consumed by that specific AI move.</li>
                  <li><b>Heap Delta</b>: Approximate heap memory change caused by that specific AI move.</li>
                  <li><b>Chosen Move</b>: Human-readable position of the selected move.</li>
                  <li><b>Legal Moves</b>: Number of legal options available at decision time.</li>
                  <li><b>Phase</b>: Opening, Midgame, or Endgame classification.</li>
                  <li><b>Outcome</b>: Final result of the game that move belonged to, once the game is finished, such as Human Win, Computer Win, X Win, O Win, or Draw.</li>
                </ul>

                <h3>Summary</h3>
                <ul>
                  <li><b>Algorithm</b>: Aggregated algorithm profile.</li>
                  <li><b>Moves</b>: Number of AI moves included in that aggregate row.</li>
                  <li><b>Avg Response (ms)</b>: Average AI think time for that algorithm profile.</li>
                  <li><b>Avg CPU Delta</b>: Average CPU consumption for moves under that profile.</li>
                  <li><b>Avg Heap Delta (MB)</b>: Average heap memory change per move.</li>
                  <li><b>Avg Confidence</b>: Mean confidence indicator, mainly meaningful for MCTS visit dominance.</li>
                  <li><b>Results</b>: Aggregated final-result counts associated with moves recorded for that algorithm profile.</li>
                </ul>
                </body>
                </html>
                """;
        HtmlContentDialog dialog = new HtmlContentDialog(this, "Metrics Documentation", html, 860, 760);
        dialog.setVisible(true);
    }

    private String buildBoardSignature(GameSnapshot snapshot) {
        StringBuilder builder = new StringBuilder(snapshot.board().getCellCount() + 32);
        builder.append(snapshot.board().getCurrentPlayer().getSymbol())
                .append('|')
                .append(snapshot.board().getSize())
                .append('|')
                .append(snapshot.board().getWinLength())
                .append('|')
                .append(snapshot.gameOver());
        for (int index = 0; index < snapshot.board().getCellCount(); index++) {
            builder.append(snapshot.board().getCell(index).getSymbol());
        }
        return builder.toString();
    }
}
