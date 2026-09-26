package admin;

import models.DisasterType;
import models.Incident;
import patterns.DisasterManagementSystem;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Custom Swing component that renders a modern bar visualization
 * of incident counts categorized by disaster type without external charting libraries.
 */
public class DisasterBarChartPanel extends JPanel {
    private final DisasterManagementSystem system;

    // Distinct modern flat colors for disaster bars
    private static final Map<DisasterType, Color> TYPE_COLORS = new EnumMap<>(DisasterType.class);

    static {
        TYPE_COLORS.put(DisasterType.FIRE, new Color(239, 68, 68));         // Red
        TYPE_COLORS.put(DisasterType.FLOOD, new Color(59, 130, 246));       // Blue
        TYPE_COLORS.put(DisasterType.ACCIDENT, new Color(245, 158, 11));    // Amber
        TYPE_COLORS.put(DisasterType.EARTHQUAKE, new Color(139, 92, 246));  // Purple
    }

    public DisasterBarChartPanel() {
        this.system = DisasterManagementSystem.getInstance();
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(460, 280));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Enable high-quality anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Tally incident counts per type
        Map<DisasterType, Integer> counts = new EnumMap<>(DisasterType.class);
        for (DisasterType type : DisasterType.values()) {
            counts.put(type, 0);
        }
        for (Incident inc : system.getAllIncidents()) {
            counts.put(inc.getDisasterType(), counts.get(inc.getDisasterType()) + 1);
        }

        int maxCount = 5;
        for (int c : counts.values()) {
            if (c > maxCount) maxCount = c;
        }

        // Layout Margins
        int marginTop = 40;
        int marginBottom = 50;
        int marginLeft = 50;
        int marginRight = 30;

        int chartWidth = width - marginLeft - marginRight;
        int chartHeight = height - marginTop - marginBottom;

        // Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(new Color(15, 23, 42));
        g2.drawString("Disaster Distribution by Category", marginLeft, 24);

        // Draw horizontal grid lines & labels
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(new Color(226, 232, 240));
        int gridSteps = 4;
        for (int i = 0; i <= gridSteps; i++) {
            int yVal = (int) Math.round((double) maxCount * i / gridSteps);
            int yPos = marginTop + chartHeight - (int) ((double) i / gridSteps * chartHeight);

            // Grid line
            g2.setColor(new Color(241, 245, 249));
            g2.drawLine(marginLeft, yPos, marginLeft + chartWidth, yPos);

            // Axis Label
            g2.setColor(new Color(148, 163, 184));
            g2.drawString(String.valueOf(yVal), marginLeft - 26, yPos + 4);
        }

        // Draw Bars
        DisasterType[] types = DisasterType.values();
        int numBars = types.length;
        int slotWidth = chartWidth / numBars;
        int barWidth = Math.min(55, slotWidth - 24);

        for (int i = 0; i < numBars; i++) {
            DisasterType type = types[i];
            int count = counts.get(type);
            int barHeight = (int) Math.round(((double) count / maxCount) * chartHeight);

            int x = marginLeft + (i * slotWidth) + (slotWidth - barWidth) / 2;
            int y = marginTop + chartHeight - barHeight;

            Color barColor = TYPE_COLORS.getOrDefault(type, new Color(100, 116, 139));

            // Bar body
            g2.setColor(barColor);
            g2.fillRoundRect(x, y, barWidth, barHeight, 6, 6);

            // Value text on top of bar
            g2.setColor(new Color(15, 23, 42));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            String countStr = String.valueOf(count);
            FontMetrics fm = g2.getFontMetrics();
            int strWidth = fm.stringWidth(countStr);
            g2.drawString(countStr, x + (barWidth - strWidth) / 2, y - 6);

            // Category Label below axis
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(71, 85, 105));
            String label = type.name().substring(0, 1) + type.name().substring(1).toLowerCase();
            int labelWidth = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, x + (barWidth - labelWidth) / 2, marginTop + chartHeight + 18);
        }

        // Bottom baseline
        g2.setColor(new Color(203, 213, 225));
        g2.drawLine(marginLeft, marginTop + chartHeight, marginLeft + chartWidth, marginTop + chartHeight);

        g2.dispose();
    }
}
