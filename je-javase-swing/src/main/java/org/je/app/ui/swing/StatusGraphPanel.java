/**
 *  MicroEmulator
 *  Copyright (C) 2001-2024 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */

package org.je.app.ui.swing;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * A real-time graph panel for displaying system metrics like CPU usage, memory usage, etc.
 */
public class StatusGraphPanel extends JPanel {
    
    private static final long serialVersionUID = 1L;
    
    private final List<DataPoint> dataPoints = new ArrayList<>();
    private final int maxDataPoints;
    private final Color lineColor;
    private final Color fillColor;
    private final String title;
    private final double maxValue;
    private final boolean showGrid;
    
    public static class DataPoint {
        public final long timestamp;
        public final double value;
        
        public DataPoint(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
    
    public StatusGraphPanel(String title, Color lineColor, Color fillColor, int maxDataPoints, double maxValue, boolean showGrid) {
        this.title = title;
        this.lineColor = lineColor;
        this.fillColor = fillColor;
        this.maxDataPoints = maxDataPoints;
        this.maxValue = maxValue;
        this.showGrid = showGrid;
        
        setPreferredSize(new Dimension(200, 150));
        setMinimumSize(new Dimension(150, 100));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }
    
    public void addDataPoint(double value) {
        synchronized (dataPoints) {
            // Add timestamp with current time for accurate real-time display
            long currentTime = System.currentTimeMillis();
            dataPoints.add(new DataPoint(currentTime, value));
            
            // Remove old data points if we exceed the maximum
            while (dataPoints.size() > maxDataPoints) {
                dataPoints.remove(0);
            }
        }
        
        // Force immediate repaint for real-time updates
        SwingUtilities.invokeLater(() -> {
            repaint();
        });
    }
    
    public void clearData() {
        synchronized (dataPoints) {
            dataPoints.clear();
        }
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Get dimensions
        int width = getWidth() - 10;
        int height = getHeight() - 25;
        int xOffset = 5;
        int yOffset = 20;
        
        // Draw title using system default font (adapts to themes)
        g2d.setColor(getForeground());
        Font titleFont = getFont().deriveFont(Font.PLAIN, 10);
        g2d.setFont(titleFont);
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, (getWidth() - titleWidth) / 2, 12);
        
        synchronized (dataPoints) {
            if (dataPoints.isEmpty()) {
                // Draw empty graph message using system default font
                g2d.setColor(getForeground().darker());
                Font messageFont = getFont().deriveFont(Font.PLAIN, 8);
                g2d.setFont(messageFont);
                String message = "No data available";
                int messageWidth = g2d.getFontMetrics().stringWidth(message);
                g2d.drawString(message, (getWidth() - messageWidth) / 2, getHeight() / 2);
                return;
            }
            
            // Find max value for scaling
            double actualMaxValue = maxValue;
            if (actualMaxValue <= 0) {
                actualMaxValue = dataPoints.stream()
                    .mapToDouble(dp -> dp.value)
                    .max()
                    .orElse(100);
            }
            
            // Add some padding to the max value to show small changes better
            if (actualMaxValue > 0) {
                actualMaxValue = actualMaxValue * 1.1; // Add 10% padding
            }
            
            // Ensure minimum range for better visibility
            if (actualMaxValue < 10) {
                actualMaxValue = 10;
            }
            
            // Draw grid if enabled
            if (showGrid) {
                drawGrid(g2d, xOffset, yOffset, width, height, actualMaxValue);
            }
            
            // Draw axis labels
            drawAxisLabels(g2d, xOffset, yOffset, width, height, actualMaxValue);
            
            // Draw the graph
            if (dataPoints.size() > 1) {
                drawGraph(g2d, xOffset, yOffset, width, height, actualMaxValue);
            }
        }
        
        g2d.dispose();
    }
    
    private void drawGrid(Graphics2D g2d, int xOffset, int yOffset, int width, int height, double maxValue) {
        Color gridColor = getForeground().darker().darker();
        g2d.setColor(gridColor);
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{2, 2}, 0));
        
        // Vertical grid lines
        for (int i = 0; i <= 10; i++) {
            int x = xOffset + (i * width) / 10;
            g2d.drawLine(x, yOffset, x, yOffset + height);
        }
        
        // Horizontal grid lines
        for (int i = 0; i <= 5; i++) {
            int y = yOffset + (i * height) / 5;
            g2d.drawLine(xOffset, y, xOffset + width, y);
        }
    }
    
    private void drawAxisLabels(Graphics2D g2d, int xOffset, int yOffset, int width, int height, double maxValue) {
        g2d.setColor(getForeground());
        Font labelFont = getFont().deriveFont(Font.PLAIN, 7);
        g2d.setFont(labelFont);
        
        // Y-axis labels
        for (int i = 0; i <= 5; i++) {
            int y = yOffset + (i * height) / 5;
            double value = maxValue - (i * maxValue) / 5;
            String label = String.format("%.0f", value);
            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g2d.drawString(label, xOffset - labelWidth - 3, y + fm.getAscent() / 2);
        }
        
        // X-axis labels (time)
        g2d.drawString("0s", xOffset, yOffset + height + 10);
        
        synchronized (dataPoints) {
            if (dataPoints.size() > 1) {
                // Show the most recent timestamp
                long mostRecentTime = dataPoints.get(dataPoints.size() - 1).timestamp;
                long oldestTime = dataPoints.get(0).timestamp;
                long timeSpan = mostRecentTime - oldestTime;
                
                if (timeSpan > 0) {
                    String timeLabel = String.format("%.1fs", timeSpan / 1000.0);
                    int timeLabelWidth = g2d.getFontMetrics().stringWidth(timeLabel);
                    g2d.drawString(timeLabel, xOffset + width - timeLabelWidth, yOffset + height + 10);
                }
            }
        }
    }
    
    private void drawGraph(Graphics2D g2d, int xOffset, int yOffset, int width, int height, double maxValue) {
        synchronized (dataPoints) {
            if (dataPoints.size() < 2) return;
            
            // Create path for the line
            Path2D path = new Path2D.Double();
            Path2D fillPath = new Path2D.Double();
            
            boolean first = true;
            long startTime = dataPoints.get(0).timestamp;
            long endTime = dataPoints.get(dataPoints.size() - 1).timestamp;
            long timeRange = Math.max(1, endTime - startTime);
            
            // Move to first point
            for (DataPoint dp : dataPoints) {
                double x = xOffset + (double)(dp.timestamp - startTime) * width / timeRange;
                double y = yOffset + height - (dp.value * height / maxValue);
                
                y = Math.max(yOffset, Math.min(yOffset + height, y));
                
                if (first) {
                    path.moveTo(x, y);
                    fillPath.moveTo(x, yOffset + height);
                    fillPath.lineTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                    fillPath.lineTo(x, y);
                }
            }
            
            if (!dataPoints.isEmpty()) {
                DataPoint last = dataPoints.get(dataPoints.size() - 1);
                double lastX = xOffset + (double)(last.timestamp - startTime) * width / timeRange;
                fillPath.lineTo(lastX, yOffset + height);
                fillPath.closePath();
            }
            
            // Draw fill
            if (fillColor != null) {
                g2d.setColor(fillColor);
                g2d.fill(fillPath);
            }
            
            // Draw line
            g2d.setColor(lineColor);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.draw(path);
            
            // Draw data points
            g2d.setColor(lineColor);
            for (DataPoint dp : dataPoints) {
                double x = xOffset + (double)(dp.timestamp - startTime) * width / timeRange;
                double y = yOffset + height - (dp.value * height / maxValue);
                
                g2d.fillOval((int)x - 2, (int)y - 2, 4, 4);
            }
        }
    }
} 