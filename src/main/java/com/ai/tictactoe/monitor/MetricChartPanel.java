package com.ai.tictactoe.monitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public final class MetricChartPanel extends JPanel {

    private static final int MAX_POINTS = 90;

    private final List<Double> values;
    private final Color lineColor;
    private final Color fillColor;
    private final double maxValue;
    private final String label;

    public MetricChartPanel(String label, Color lineColor, Color fillColor, double maxValue) {
        this.label = label;
        this.lineColor = lineColor;
        this.fillColor = fillColor;
        this.maxValue = maxValue;
        this.values = new ArrayList<>();
        setPreferredSize(new Dimension(260, 120));
        setOpaque(false);
    }

    public void addValue(double value) {
        if (values.size() == MAX_POINTS) {
            values.remove(0);
        }
        values.add(Math.max(0.0, Math.min(maxValue, value)));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            g2.setColor(new Color(255, 255, 255, 20));
            for (int i = 1; i <= 3; i++) {
                int y = (height * i) / 4;
                g2.drawLine(0, y, width, y);
            }

            g2.setColor(new Color(255, 255, 255, 160));
            g2.drawString(label, 10, 16);

            if (values.size() < 2) {
                return;
            }

            Path2D.Double linePath = new Path2D.Double();
            Path2D.Double fillPath = new Path2D.Double();
            for (int i = 0; i < values.size(); i++) {
                double normalized = values.get(i) / maxValue;
                double x = (double) i * (width - 1) / (MAX_POINTS - 1);
                double y = height - 8 - normalized * (height - 24);
                if (i == 0) {
                    linePath.moveTo(x, y);
                    fillPath.moveTo(x, height - 8);
                    fillPath.lineTo(x, y);
                } else {
                    linePath.lineTo(x, y);
                    fillPath.lineTo(x, y);
                }
            }

            double lastX = (double) (values.size() - 1) * (width - 1) / (MAX_POINTS - 1);
            fillPath.lineTo(lastX, height - 8);
            fillPath.closePath();

            g2.setColor(fillColor);
            g2.fill(fillPath);
            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(linePath);
        } finally {
            g2.dispose();
        }
    }
}
