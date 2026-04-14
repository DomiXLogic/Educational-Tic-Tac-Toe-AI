package com.ai.tictactoe.monitor;

import com.ai.tictactoe.controller.GameSnapshot;
import com.ai.tictactoe.telemetry.AiMoveTelemetry;
import com.ai.tictactoe.telemetry.MoveCandidate;
import com.ai.tictactoe.telemetry.ProcessSnapshot;
import com.ai.tictactoe.telemetry.ProcessTelemetry;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public final class AlgorithmMonitorFrame extends JFrame implements TelemetrySink {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private final JLabel modeValueLabel;
    private final JLabel boardValueLabel;
    private final JLabel goalValueLabel;
    private final JLabel turnValueLabel;
    private final JLabel startValueLabel;
    private final JLabel simulationsValueLabel;
    private final JLabel stupidityValueLabel;

    private final JLabel cpuValueLabel;
    private final JLabel heapValueLabel;
    private final JLabel threadsValueLabel;
    private final JLabel gcValueLabel;
    private final MetricChartPanel cpuChartPanel;
    private final MetricChartPanel heapChartPanel;

    private final JLabel algorithmValueLabel;
    private final JLabel responseValueLabel;
    private final JLabel moveValueLabel;
    private final JLabel legalMovesValueLabel;
    private final JLabel phaseValueLabel;
    private final JLabel timestampValueLabel;
    private final JLabel cpuDeltaValueLabel;
    private final JLabel heapDeltaValueLabel;

    private final JLabel nodesValueLabel;
    private final JLabel cutoffsValueLabel;
    private final JLabel depthValueLabel;
    private final JLabel heuristicValueLabel;
    private final JLabel simulationsMetricValueLabel;
    private final JLabel simsPerSecondValueLabel;
    private final JLabel rolloutValueLabel;
    private final JLabel confidenceValueLabel;

    private final CandidateTableModel candidateTableModel;
    private final MoveHistoryTableModel moveHistoryTableModel;
    private final SummaryTableModel summaryTableModel;
    private final Timer systemTimer;
    private final List<MoveHistoryRow> historyRows;
    private GameSnapshot latestSnapshot;

    public AlgorithmMonitorFrame() {
        super("Algorithm Monitor");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setLocationByPlatform(true);

        modeValueLabel = createValueLabel();
        boardValueLabel = createValueLabel();
        goalValueLabel = createValueLabel();
        turnValueLabel = createValueLabel();
        startValueLabel = createValueLabel();
        simulationsValueLabel = createValueLabel();
        stupidityValueLabel = createValueLabel();

        cpuValueLabel = createValueLabel();
        heapValueLabel = createValueLabel();
        threadsValueLabel = createValueLabel();
        gcValueLabel = createValueLabel();
        cpuChartPanel = new MetricChartPanel("Process CPU %", new java.awt.Color(88, 86, 214), new java.awt.Color(88, 86, 214, 48), 100.0);
        heapChartPanel = new MetricChartPanel("Heap Used (MB)", new java.awt.Color(152, 152, 0), new java.awt.Color(152, 152, 0, 48), 1024.0);

        algorithmValueLabel = createValueLabel();
        responseValueLabel = createValueLabel();
        moveValueLabel = createValueLabel();
        legalMovesValueLabel = createValueLabel();
        phaseValueLabel = createValueLabel();
        timestampValueLabel = createValueLabel();
        cpuDeltaValueLabel = createValueLabel();
        heapDeltaValueLabel = createValueLabel();

        nodesValueLabel = createValueLabel();
        cutoffsValueLabel = createValueLabel();
        depthValueLabel = createValueLabel();
        heuristicValueLabel = createValueLabel();
        simulationsMetricValueLabel = createValueLabel();
        simsPerSecondValueLabel = createValueLabel();
        rolloutValueLabel = createValueLabel();
        confidenceValueLabel = createValueLabel();

        candidateTableModel = new CandidateTableModel();
        moveHistoryTableModel = new MoveHistoryTableModel();
        summaryTableModel = new SummaryTableModel();
        historyRows = new ArrayList<>();

        setLayout(new BorderLayout(12, 12));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);

        systemTimer = new Timer(800, event -> refreshSystemMetrics());
    }

    public void updateSnapshot(GameSnapshot snapshot) {
        latestSnapshot = snapshot;
        if (!isVisible()) {
            return;
        }
        modeValueLabel.setText(snapshot.gameMode().toString());
        boardValueLabel.setText(snapshot.board().getSize() + "x" + snapshot.board().getSize());
        goalValueLabel.setText(snapshot.board().getWinLength() + " in a row");
        turnValueLabel.setText(snapshot.gameOver()
                ? "Game Over"
                : snapshot.gameMode().isPcVsPc()
                ? (snapshot.board().getCurrentPlayer() == com.ai.tictactoe.model.Player.X ? "X Bot" : "O Bot")
                : snapshot.board().getCurrentPlayer() == com.ai.tictactoe.model.Player.X ? "Human (X)" : "Computer (O)");
        startValueLabel.setText(snapshot.gameMode().isPcVsPc() ? "X (fixed)" : snapshot.startMode().toString());
        simulationsValueLabel.setText(snapshot.gameMode().isPcVsPc()
                ? buildPcVsPcSimulationSummary(snapshot)
                : snapshot.gameMode() == com.ai.tictactoe.model.GameMode.HUMAN_VS_AI_MCTS
                ? Integer.toString(snapshot.mctsSimulationCount())
                : "N/A");
        stupidityValueLabel.setText(snapshot.gameMode().isPcVsPc()
                ? buildPcVsPcStupiditySummary(snapshot)
                : snapshot.gameMode() == com.ai.tictactoe.model.GameMode.HUMAN_VS_AI_MCTS
                ? snapshot.mctsArtificialStupidityEnabled() ? snapshot.mctsStupidityLevel().getDisplayName() : "Off"
                : "N/A");

        if (snapshot.gameOver()) {
            String outcome;
            if (snapshot.board().isDraw()) {
                outcome = "Draw";
            } else if (snapshot.gameMode().isPcVsPc()) {
                outcome = snapshot.board().getWinner() == com.ai.tictactoe.model.Player.X ? "X Win" : "O Win";
            } else {
                outcome = snapshot.board().getWinner() == com.ai.tictactoe.model.Player.X ? "Human Win" : "Computer Win";
            }
            recordGameOutcome(snapshot.gameId(), outcome);
        }
    }

    @Override
    public void recordAiMove(long gameId, AiMoveTelemetry telemetry) {
        MoveHistoryRow row = new MoveHistoryRow(gameId, telemetry);
        historyRows.add(row);
        if (isVisible()) {
            applyTelemetryToView(telemetry);
            moveHistoryTableModel.setRows(historyRows);
            summaryTableModel.setRows(historyRows);
        }
    }

    @Override
    public void recordGameOutcome(long gameId, String outcome) {
        boolean changed = false;
        for (MoveHistoryRow row : historyRows) {
            if (row.gameId == gameId && !outcome.equals(row.outcome)) {
                row.outcome = outcome;
                changed = true;
            }
        }
        if (changed) {
            if (isVisible()) {
                moveHistoryTableModel.fireTableDataChanged();
                summaryTableModel.setRows(historyRows);
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        boolean wasVisible = isVisible();
        super.setVisible(visible);
        if (visible) {
            if (!wasVisible) {
                systemTimer.start();
            }
            if (latestSnapshot != null) {
                updateSnapshot(latestSnapshot);
            }
            moveHistoryTableModel.setRows(historyRows);
            summaryTableModel.setRows(historyRows);
            refreshSystemMetrics();
        } else {
            systemTimer.stop();
        }
    }

    private JPanel buildTopPanel() {
        JPanel panel = createSectionPanel("Current Game");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 18, 8));
        panel.add(labeledValue("Mode", modeValueLabel));
        panel.add(labeledValue("Board", boardValueLabel));
        panel.add(labeledValue("Goal", goalValueLabel));
        panel.add(labeledValue("Turn", turnValueLabel));
        panel.add(labeledValue("Starts", startValueLabel));
        panel.add(labeledValue("Simulations", simulationsValueLabel));
        panel.add(labeledValue("Stupidity", stupidityValueLabel));
        return panel;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Live View", buildLiveViewPanel());
        tabs.addTab("Move History", buildHistoryPanel());
        tabs.addTab("Summary", buildSummaryPanel());
        return tabs;
    }

    private JPanel buildLiveViewPanel() {
        JPanel systemPanel = createSectionPanel("System Metrics");
        systemPanel.setLayout(new BoxLayout(systemPanel, BoxLayout.Y_AXIS));
        systemPanel.add(labeledValue("CPU", cpuValueLabel));
        systemPanel.add(labeledValue("Heap", heapValueLabel));
        systemPanel.add(labeledValue("Threads", threadsValueLabel));
        systemPanel.add(labeledValue("GC", gcValueLabel));
        systemPanel.add(cpuChartPanel);
        systemPanel.add(heapChartPanel);

        JPanel analyticsPanel = createSectionPanel("Current Move Analytics");
        analyticsPanel.setLayout(new BoxLayout(analyticsPanel, BoxLayout.Y_AXIS));
        analyticsPanel.add(labeledValue("Algorithm", algorithmValueLabel));
        analyticsPanel.add(labeledValue("Response", responseValueLabel));
        analyticsPanel.add(labeledValue("Chosen Move", moveValueLabel));
        analyticsPanel.add(labeledValue("Legal Moves", legalMovesValueLabel));
        analyticsPanel.add(labeledValue("Phase", phaseValueLabel));
        analyticsPanel.add(labeledValue("Timestamp", timestampValueLabel));
        analyticsPanel.add(labeledValue("CPU Delta", cpuDeltaValueLabel));
        analyticsPanel.add(labeledValue("Heap Delta", heapDeltaValueLabel));

        JPanel internalsPanel = createSectionPanel("Algorithm Internals");
        internalsPanel.setLayout(new BorderLayout(8, 8));
        JPanel metricsPanel = new JPanel(new java.awt.GridLayout(4, 2, 8, 8));
        metricsPanel.add(labeledValue("Nodes / Steps", nodesValueLabel));
        metricsPanel.add(labeledValue("Alpha-Beta Cutoffs", cutoffsValueLabel));
        metricsPanel.add(labeledValue("Max Depth", depthValueLabel));
        metricsPanel.add(labeledValue("Heuristic Score", heuristicValueLabel));
        metricsPanel.add(labeledValue("Simulations", simulationsMetricValueLabel));
        metricsPanel.add(labeledValue("Simulations/sec", simsPerSecondValueLabel));
        metricsPanel.add(labeledValue("Rollouts", rolloutValueLabel));
        metricsPanel.add(labeledValue("Confidence", confidenceValueLabel));
        internalsPanel.add(metricsPanel, BorderLayout.NORTH);
        internalsPanel.add(createTableScrollPane(new JTable(candidateTableModel)), BorderLayout.CENTER);

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, systemPanel, analyticsPanel);
        horizontalSplit.setResizeWeight(0.32);
        JSplitPane rootSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, horizontalSplit, internalsPanel);
        rootSplit.setResizeWeight(0.62);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(rootSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(moveHistoryTableModel);
        configureTable(table);
        panel.add(createTableScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(summaryTableModel);
        configureTable(table);
        panel.add(createTableScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private JPanel labeledValue(String labelText, JLabel valueLabel) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JLabel label = new JLabel(labelText + ":");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label);
        panel.add(valueLabel);
        return panel;
    }

    private JLabel createValueLabel() {
        JLabel label = new JLabel("-");
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private JScrollPane createTableScrollPane(JTable table) {
        configureTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private void configureTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);
    }

    private void refreshSystemMetrics() {
        if (!isVisible()) {
            return;
        }
        ProcessSnapshot snapshot = ProcessTelemetry.capture();
        cpuValueLabel.setText(DECIMAL_FORMAT.format(snapshot.processCpuLoadPercent()) + "%");
        heapValueLabel.setText(formatBytes(snapshot.heapUsedBytes()) + " / " + formatBytes(snapshot.heapMaxBytes()));
        threadsValueLabel.setText(Integer.toString(snapshot.threadCount()));
        gcValueLabel.setText(snapshot.gcCount() + " collections / " + snapshot.gcTimeMillis() + " ms");
        cpuChartPanel.addValue(snapshot.processCpuLoadPercent());
        heapChartPanel.addValue(snapshot.heapUsedBytes() / (1024.0 * 1024.0));
    }

    private void applyTelemetryToView(AiMoveTelemetry telemetry) {
        algorithmValueLabel.setText(telemetry.algorithmName() + (telemetry.exactSearch() ? " (Exact)" : ""));
        responseValueLabel.setText(telemetry.responseTimeMillis() + " ms");
        moveValueLabel.setText(formatMove(telemetry.chosenRow(), telemetry.chosenColumn(), telemetry.chosenMove()));
        legalMovesValueLabel.setText(Integer.toString(telemetry.legalMoves()));
        phaseValueLabel.setText(telemetry.gamePhase());
        timestampValueLabel.setText(TIME_FORMAT.format(new Date(telemetry.timestampMillis())));
        cpuDeltaValueLabel.setText(formatCpuDelta(telemetry.processBefore(), telemetry.processAfter(), telemetry.responseTimeMillis()));
        heapDeltaValueLabel.setText(formatBytesDelta(telemetry.processBefore(), telemetry.processAfter()));

        nodesValueLabel.setText(telemetry.nodesExplored() > 0 ? Long.toString(telemetry.nodesExplored()) : "-");
        cutoffsValueLabel.setText(telemetry.alphaBetaCutoffs() > 0 ? Long.toString(telemetry.alphaBetaCutoffs()) : "-");
        depthValueLabel.setText(telemetry.maxDepthReached() > 0 ? Integer.toString(telemetry.maxDepthReached()) : "-");
        heuristicValueLabel.setText(Math.abs(telemetry.heuristicScore()) > 0.0001
                ? DECIMAL_FORMAT.format(telemetry.heuristicScore())
                : "-");
        simulationsMetricValueLabel.setText(telemetry.simulations() > 0 ? Long.toString(telemetry.simulations()) : "-");
        simsPerSecondValueLabel.setText(telemetry.simulationsPerSecond() > 0.0
                ? DECIMAL_FORMAT.format(telemetry.simulationsPerSecond())
                : "-");
        rolloutValueLabel.setText(telemetry.averageRolloutLength() > 0.0
                ? DECIMAL_FORMAT.format(telemetry.averageRolloutLength()) + " moves / "
                        + DECIMAL_FORMAT.format(telemetry.averageRolloutTimeMillis()) + " ms"
                : "-");
        confidenceValueLabel.setText(telemetry.bestMoveVisitRatio() > 0.0
                ? DECIMAL_FORMAT.format(telemetry.bestMoveVisitRatio() * 100.0) + "%"
                : "-");

        candidateTableModel.setCandidates(telemetry.candidateMoves());
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "-";
        }
        return DECIMAL_FORMAT.format(bytes / (1024.0 * 1024.0)) + " MB";
    }

    private String formatBytesDelta(ProcessSnapshot before, ProcessSnapshot after) {
        if (before == null || after == null) {
            return "-";
        }
        long delta = after.heapUsedBytes() - before.heapUsedBytes();
        String prefix = delta > 0 ? "+" : "";
        return prefix + formatBytes(delta);
    }

    private String formatCpuDelta(ProcessSnapshot before, ProcessSnapshot after, long responseTimeMillis) {
        if (before == null || after == null || responseTimeMillis <= 0) {
            return "-";
        }
        long cpuNanos = Math.max(0L, after.processCpuTimeNanos() - before.processCpuTimeNanos());
        double cpuLoad = cpuNanos / (responseTimeMillis * 1_000_000.0) * 100.0;
        return DECIMAL_FORMAT.format(cpuLoad) + "%";
    }

    private String formatMove(int row, int column, int moveIndex) {
        return "r" + (row + 1) + " c" + (column + 1) + " (#" + moveIndex + ")";
    }

    private String buildPcVsPcSimulationSummary(GameSnapshot snapshot) {
        return "X " + (snapshot.xBotStrategy() == com.ai.tictactoe.model.BotStrategyType.MCTS
                ? snapshot.xMctsSimulationCount()
                : "N/A")
                + " / O " + (snapshot.oBotStrategy() == com.ai.tictactoe.model.BotStrategyType.MCTS
                ? snapshot.oMctsSimulationCount()
                : "N/A");
    }

    private String buildPcVsPcStupiditySummary(GameSnapshot snapshot) {
        return "X " + (snapshot.xBotStrategy() == com.ai.tictactoe.model.BotStrategyType.MCTS
                ? snapshot.xMctsArtificialStupidityEnabled() ? snapshot.xMctsStupidityLevel().getDisplayName() : "Off"
                : "N/A")
                + " / O " + (snapshot.oBotStrategy() == com.ai.tictactoe.model.BotStrategyType.MCTS
                ? snapshot.oMctsArtificialStupidityEnabled() ? snapshot.oMctsStupidityLevel().getDisplayName() : "Off"
                : "N/A");
    }

    private static final class CandidateTableModel extends AbstractTableModel {

        private final String[] columns = {"Move", "Row", "Col", "Visits", "Avg Score", "Eval"};
        private List<MoveCandidate> candidates = List.of();

        void setCandidates(List<MoveCandidate> candidates) {
            this.candidates = candidates == null ? List.of() : List.copyOf(candidates);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return candidates.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            MoveCandidate candidate = candidates.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> candidate.moveIndex();
                case 1 -> candidate.row() + 1;
                case 2 -> candidate.column() + 1;
                case 3 -> candidate.visits() == 0 ? "-" : candidate.visits();
                case 4 -> Math.abs(candidate.averageScore()) < 0.0001 ? "-" : DECIMAL_FORMAT.format(candidate.averageScore());
                case 5 -> DECIMAL_FORMAT.format(candidate.evaluationScore());
                default -> "";
            };
        }
    }

    private static final class MoveHistoryTableModel extends AbstractTableModel {

        private final String[] columns = {
            "Game", "Move #", "Algorithm", "Response (ms)", "CPU Delta", "Heap Delta", "Chosen Move", "Legal Moves", "Phase", "Outcome"
        };
        private List<MoveHistoryRow> rows = List.of();

        void setRows(List<MoveHistoryRow> rows) {
            this.rows = List.copyOf(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            MoveHistoryRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.gameId;
                case 1 -> row.moveNumber;
                case 2 -> row.algorithmLabel;
                case 3 -> row.responseTimeMillis;
                case 4 -> row.cpuDeltaText;
                case 5 -> row.heapDeltaText;
                case 6 -> row.moveText;
                case 7 -> row.legalMoves;
                case 8 -> row.phase;
                case 9 -> row.outcome;
                default -> "";
            };
        }
    }

    private static final class SummaryTableModel extends AbstractTableModel {

        private final String[] columns = {
            "Algorithm", "Moves", "Avg Response (ms)", "Avg CPU Delta", "Avg Heap Delta (MB)", "Avg Confidence", "Results"
        };
        private List<SummaryRow> rows = List.of();

        void setRows(List<MoveHistoryRow> historyRows) {
            Map<String, List<MoveHistoryRow>> grouped = new LinkedHashMap<>();
            for (MoveHistoryRow row : historyRows) {
                grouped.computeIfAbsent(row.algorithmLabel, key -> new ArrayList<>()).add(row);
            }

            rows = grouped.entrySet().stream()
                    .map(entry -> SummaryRow.from(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(SummaryRow::algorithmLabel))
                    .toList();
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SummaryRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.algorithmLabel();
                case 1 -> row.moves();
                case 2 -> DECIMAL_FORMAT.format(row.averageResponseMillis());
                case 3 -> DECIMAL_FORMAT.format(row.averageCpuDeltaPercent()) + "%";
                case 4 -> DECIMAL_FORMAT.format(row.averageHeapDeltaMb());
                case 5 -> row.averageConfidencePercent() <= 0.0 ? "-" : DECIMAL_FORMAT.format(row.averageConfidencePercent()) + "%";
                case 6 -> row.resultSummary();
                default -> "";
            };
        }
    }

    private static final class MoveHistoryRow {

        private final long gameId;
        private final int moveNumber;
        private final String algorithmLabel;
        private final long responseTimeMillis;
        private final double cpuDeltaPercent;
        private final double heapDeltaMb;
        private final String cpuDeltaText;
        private final String heapDeltaText;
        private final String moveText;
        private final int legalMoves;
        private final String phase;
        private final double confidencePercent;
        private String outcome;

        private MoveHistoryRow(long gameId, AiMoveTelemetry telemetry) {
            this.gameId = gameId;
            this.moveNumber = telemetry.occupiedCells() + 1;
            this.algorithmLabel = telemetry.algorithmName()
                    + (telemetry.algorithmName().equals("Minimax")
                    ? telemetry.exactSearch() ? " (Exact)" : " (Heuristic)"
                    : "");
            this.responseTimeMillis = telemetry.responseTimeMillis();
            this.cpuDeltaPercent = computeCpuDeltaPercent(telemetry.processBefore(), telemetry.processAfter(), telemetry.responseTimeMillis());
            this.heapDeltaMb = computeHeapDeltaMb(telemetry.processBefore(), telemetry.processAfter());
            this.cpuDeltaText = DECIMAL_FORMAT.format(cpuDeltaPercent) + "%";
            this.heapDeltaText = DECIMAL_FORMAT.format(heapDeltaMb) + " MB";
            this.moveText = "r" + (telemetry.chosenRow() + 1) + " c" + (telemetry.chosenColumn() + 1);
            this.legalMoves = telemetry.legalMoves();
            this.phase = telemetry.gamePhase();
            this.confidencePercent = telemetry.bestMoveVisitRatio() * 100.0;
            this.outcome = "In Progress";
        }

        private static double computeCpuDeltaPercent(ProcessSnapshot before, ProcessSnapshot after, long responseTimeMillis) {
            if (before == null || after == null || responseTimeMillis <= 0) {
                return 0.0;
            }
            long cpuNanos = Math.max(0L, after.processCpuTimeNanos() - before.processCpuTimeNanos());
            return cpuNanos / (responseTimeMillis * 1_000_000.0) * 100.0;
        }

        private static double computeHeapDeltaMb(ProcessSnapshot before, ProcessSnapshot after) {
            if (before == null || after == null) {
                return 0.0;
            }
            return (after.heapUsedBytes() - before.heapUsedBytes()) / (1024.0 * 1024.0);
        }
    }

    private record SummaryRow(
            String algorithmLabel,
            int moves,
            double averageResponseMillis,
            double averageCpuDeltaPercent,
            double averageHeapDeltaMb,
            double averageConfidencePercent,
            String resultSummary) {

        static SummaryRow from(String algorithmLabel, List<MoveHistoryRow> rows) {
            double avgResponse = rows.stream().mapToLong(row -> row.responseTimeMillis).average().orElse(0.0);
            double avgCpu = rows.stream().mapToDouble(row -> row.cpuDeltaPercent).average().orElse(0.0);
            double avgHeap = rows.stream().mapToDouble(row -> row.heapDeltaMb).average().orElse(0.0);
            double avgConfidence = rows.stream().mapToDouble(row -> row.confidencePercent).average().orElse(0.0);
            String resultSummary = buildResultSummary(rows);
            return new SummaryRow(algorithmLabel, rows.size(), avgResponse, avgCpu, avgHeap, avgConfidence, resultSummary);
        }

        private static String buildResultSummary(List<MoveHistoryRow> rows) {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (MoveHistoryRow row : rows) {
                counts.merge(row.outcome, 1L, Long::sum);
            }
            return counts.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("-");
        }
    }
}
