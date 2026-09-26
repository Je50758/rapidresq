package gui;

import auth.Role;
import auth.User;
import exceptions.InvalidIncidentException;
import models.Incident;
import models.IncidentStatus;
import patterns.DisasterManagementSystem;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Main overview dashboard displaying metric summary cards,
 * prioritized incident queue table, citizen verification actions,
 * and reporter trust rating badges (High/Medium/Low Trust).
 */
public class DashboardPanel extends JPanel {
    private final User currentUser;
    private final DisasterManagementSystem system;

    private JLabel totalIncidentsLabel;
    private JLabel verifiedCountLabel;
    private JLabel inProgressLabel;
    private JLabel resolvedLabel;
    private JLabel teamsAvailableLabel;

    private JTable incidentTable;
    private DefaultTableModel tableModel;

    public DashboardPanel(User currentUser) {
        this.currentUser = currentUser;
        this.system = DisasterManagementSystem.getInstance();

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top: Metric Stat Cards
        add(createMetricCardsPanel(), BorderLayout.NORTH);

        // Center: Incidents Table Card
        add(createTableCardPanel(), BorderLayout.CENTER);

        // Initial Data Load
        refreshDashboardData();
    }

    private JPanel createMetricCardsPanel() {
        JPanel statsGrid = new JPanel(new GridLayout(1, 5, 12, 0));
        statsGrid.setOpaque(false);

        totalIncidentsLabel = new JLabel("0", SwingConstants.CENTER);
        verifiedCountLabel = new JLabel("0", SwingConstants.CENTER);
        inProgressLabel = new JLabel("0", SwingConstants.CENTER);
        resolvedLabel = new JLabel("0", SwingConstants.CENTER);
        teamsAvailableLabel = new JLabel("0", SwingConstants.CENTER);

        statsGrid.add(createStatCard("Total Incidents", totalIncidentsLabel, Theme.PRIMARY));
        statsGrid.add(createStatCard("Verified by Crowd", verifiedCountLabel, Theme.SUCCESS));
        statsGrid.add(createStatCard("In Progress", inProgressLabel, Theme.WARNING));
        statsGrid.add(createStatCard("Resolved & Safe", resolvedLabel, Theme.INFO));
        statsGrid.add(createStatCard("Teams Ready", teamsAvailableLabel, Theme.ACCENT));

        return statsGrid;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_SMALL);
        titleLbl.setForeground(Theme.TEXT_MUTED);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);

        return card;
    }

    private JPanel createTableCardPanel() {
        JPanel card = Theme.createCardPanel();
        card.setLayout(new BorderLayout(12, 12));

        // Header & Actions Toolbar
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel tableTitle = new JLabel("Active Incident Priority Triage & Citizen Trust Verification");
        tableTitle.setFont(Theme.FONT_SUBTITLE);
        tableTitle.setForeground(Theme.TEXT_MAIN);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton confirmBtn = Theme.createButton("👍 Confirm / Verify (+1)", Theme.SUCCESS, Color.WHITE);
        confirmBtn.setToolTipText("Citizens can crowdsource verify an unverified report");
        confirmBtn.addActionListener(e -> performCrowdConfirmation());

        JButton falseAlarmBtn = Theme.createButton("❌ Mark False / Spam (-20 Trust)", Theme.DANGER, Color.WHITE);
        falseAlarmBtn.setToolTipText("Mark incident as fake report and penalize reporter trust score");
        falseAlarmBtn.addActionListener(e -> performMarkFalseAlarm());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshDashboardData());

        btnPanel.add(confirmBtn);
        btnPanel.add(falseAlarmBtn);
        btnPanel.add(refreshBtn);

        headerPanel.add(tableTitle, BorderLayout.WEST);
        headerPanel.add(btnPanel, BorderLayout.EAST);

        // Table with Trust Badge column
        String[] columns = {"ID", "Type", "Location", "Injured", "Reports", "Severity", "Priority Score", "Status", "Assigned Team", "Reporter", "Reporter Trust Badge"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        incidentTable = new JTable(tableModel);
        incidentTable.setFont(Theme.FONT_REGULAR);
        incidentTable.setRowHeight(28);
        incidentTable.getTableHeader().setFont(Theme.FONT_BOLD);
        incidentTable.getTableHeader().setBackground(new Color(241, 245, 249));
        incidentTable.getTableHeader().setForeground(Theme.TEXT_MAIN);
        incidentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        incidentTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        incidentTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        incidentTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        incidentTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        incidentTable.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);
        incidentTable.getColumnModel().getColumn(10).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(incidentTable);
        scrollPane.setBorder(new LineBorder(Theme.BORDER, 1));

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private void performCrowdConfirmation() {
        int selectedRow = incidentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select an incident from the table to confirm.",
                    "No Incident Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String incidentId = (String) tableModel.getValueAt(selectedRow, 0);
        try {
            system.addConfirmingReport(incidentId, currentUser.getUsername());
            JOptionPane.showMessageDialog(this,
                    "Thank you! Your citizen report confirmation for " + incidentId + " has been recorded.\n" +
                    "When 3 confirmations are reached, the incident transitions to VERIFIED and the reporter receives +5 Trust Score!",
                    "Confirmation Recorded",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshDashboardData();
        } catch (InvalidIncidentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performMarkFalseAlarm() {
        int selectedRow = incidentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an incident to mark as false alarm.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String incidentId = (String) tableModel.getValueAt(selectedRow, 0);
        Incident inc = system.getIncidentById(incidentId);
        if (inc == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Mark " + incidentId + " as FALSE ALARM / SPAM?\n" +
                "Reporter @" + inc.getReporterUsername() + " will receive a -20 Trust Score penalty.",
                "Confirm Fake Report Penalty",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                system.markIncidentAsFalseAlarm(incidentId);
                JOptionPane.showMessageDialog(this,
                        "Incident marked as FALSE ALARM. Reporter @" + inc.getReporterUsername() + " penalized -20 Trust Score.",
                        "Penalty Applied",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshDashboardData();
            } catch (InvalidIncidentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshDashboardData() {
        int total = 0;
        int verified = 0;
        int inProgress = 0;
        int resolved = 0;

        List<Incident> sortedIncidents = new ArrayList<>(system.getAllIncidents());
        sortedIncidents.sort((a, b) -> Double.compare(b.getPriority(), a.getPriority()));

        tableModel.setRowCount(0);

        for (Incident inc : sortedIncidents) {
            total++;
            if (inc.getStatus() == IncidentStatus.VERIFIED) verified++;
            if (inc.getStatus() == IncidentStatus.IN_PROGRESS) inProgress++;
            if (inc.getStatus() == IncidentStatus.RESOLVED) resolved++;

            // Feature 1: Format Trust Badge
            int trust = inc.getReporterTrustScore();
            String trustBadge;
            if (trust >= 70) {
                trustBadge = "🟢 High (" + trust + "%)";
            } else if (trust >= 30) {
                trustBadge = "🟡 Med (" + trust + "%)";
            } else {
                trustBadge = "🔴 Low Trust (" + trust + "%)";
            }

            tableModel.addRow(new Object[]{
                    inc.getId(),
                    inc.getDisasterType().name(),
                    inc.getLocation(),
                    inc.getInjuredCount(),
                    inc.getReportCount(),
                    inc.getSeverity().name(),
                    String.format("%.1f", inc.getPriority()),
                    inc.getStatus().name(),
                    inc.getAssignedTeamId() != null ? inc.getAssignedTeamId() : "Unassigned",
                    inc.getReporterUsername(),
                    trustBadge
            });
        }

        long availableTeams = system.getAllResponseTeams().stream().filter(t -> t.isAvailable()).count();

        totalIncidentsLabel.setText(String.valueOf(total));
        verifiedCountLabel.setText(String.valueOf(verified));
        inProgressLabel.setText(String.valueOf(inProgress));
        resolvedLabel.setText(String.valueOf(resolved));
        teamsAvailableLabel.setText(String.valueOf(availableTeams));
    }
}
