package gui;

import auth.User;
import models.EscalationLog;
import observers.EmergencyEscalationNotifier;
import patterns.DisasterManagementSystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel rendering simulated emergency dispatch escalations
 * to the National Emergency Service (999) and Armed Forces Disaster Wing.
 */
public class EscalationLogPanel extends JPanel {
    private final User currentUser;
    private final DisasterManagementSystem system;

    private DefaultTableModel tableModel;

    public EscalationLogPanel(User currentUser) {
        this.currentUser = currentUser;
        this.system = DisasterManagementSystem.getInstance();

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("🚨 National Emergency Service (999) Escalation Console");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.DANGER);

        JLabel subtitle = new JLabel("Automated real-time escalation stream dispatched when incident severity reaches CRITICAL.");
        subtitle.setFont(Theme.FONT_REGULAR);
        subtitle.setForeground(Theme.TEXT_MUTED);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh Stream");
        refreshBtn.addActionListener(e -> refreshLogs());

        btnPanel.add(refreshBtn);

        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(subtitle, BorderLayout.SOUTH);
        headerPanel.add(btnPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Center Table Card
        JPanel card = Theme.createCardPanel();
        card.setLayout(new BorderLayout(8, 8));

        JLabel cardTitle = new JLabel("National Escalation Dispatch Logs (Audit & Transmission Grid)");
        cardTitle.setFont(Theme.FONT_SUBTITLE);
        cardTitle.setForeground(Theme.TEXT_MAIN);

        String[] cols = {"Escalation Ref", "Timestamp", "Incident ID", "Disaster Category", "Location Sector", "Priority Score", "National Agency", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setFont(Theme.FONT_REGULAR);
        table.setRowHeight(28);
        table.getTableHeader().setFont(Theme.FONT_BOLD);
        table.getTableHeader().setBackground(new Color(254, 242, 242)); // Light red tint for 999
        table.getTableHeader().setForeground(Theme.DANGER);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);
        table.getColumnModel().getColumn(5).setCellRenderer(center);
        table.getColumnModel().getColumn(7).setCellRenderer(center);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));

        card.add(cardTitle, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        refreshLogs();
    }

    public void refreshLogs() {
        tableModel.setRowCount(0);
        EmergencyEscalationNotifier notifier = system.getEscalationNotifier();
        List<EscalationLog> logs = notifier.getEscalationLogs();

        for (int i = logs.size() - 1; i >= 0; i--) { // Newest first
            EscalationLog log = logs.get(i);
            tableModel.addRow(new Object[]{
                    log.getEscalationId(),
                    log.getFormattedTimestamp(),
                    log.getIncidentId(),
                    log.getDisasterType().name(),
                    log.getLocation(),
                    String.format("%.1f", log.getPriority()),
                    log.getSimulatedAgency(),
                    log.getTransmissionStatus()
            });
        }
    }
}
