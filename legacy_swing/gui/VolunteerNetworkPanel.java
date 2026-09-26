package gui;

import auth.User;
import models.Incident;
import models.IncidentStatus;
import models.ResponseTeam;
import models.Volunteer;
import observers.VolunteerNotifier;
import patterns.DisasterManagementSystem;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Community Volunteer Hub panel.
 * Shows broadcast emergency alerts dispatched via Observer pattern,
 * and allows volunteers to coordinate first-responder field activities.
 */
public class VolunteerNetworkPanel extends JPanel {
    private final User currentUser;
    private final DisasterManagementSystem system;

    private DefaultTableModel alertTableModel;
    private DefaultTableModel volunteerTableModel;

    public VolunteerNetworkPanel(User currentUser) {
        this.currentUser = currentUser;
        this.system = DisasterManagementSystem.getInstance();

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("🤝 Community Volunteer First-Responder Hub");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Real-time disaster alerts, volunteer corps directory, and citizen verification staging.");
        subtitle.setFont(Theme.FONT_REGULAR);
        subtitle.setForeground(Theme.TEXT_MUTED);

        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(subtitle, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // Center split: Top: Broadcast Alerts, Bottom: Volunteer Units
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                createAlertsCard(),
                createVolunteerUnitsCard());
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(8);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createAlertsCard() {
        JPanel card = Theme.createCardPanel();
        card.setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lbl = new JLabel("📢 Live Emergency Broadcast Alerts (Observer Feed)");
        lbl.setFont(Theme.FONT_SUBTITLE);
        lbl.setForeground(Theme.TEXT_MAIN);

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh Alerts");
        refreshBtn.addActionListener(e -> refreshData());

        header.add(lbl, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        String[] cols = {"Alert ID", "Time", "Incident ID", "Severity", "Broadcast Alert Message"};
        alertTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(alertTableModel);
        table.setFont(Theme.FONT_REGULAR);
        table.setRowHeight(26);
        table.getTableHeader().setFont(Theme.FONT_BOLD);
        table.getTableHeader().setBackground(new Color(241, 245, 249));

        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(450);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));

        card.add(header, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel createVolunteerUnitsCard() {
        JPanel card = Theme.createCardPanel();
        card.setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lbl = new JLabel("👥 Registered Volunteer Response Corps & Sectors");
        lbl.setFont(Theme.FONT_SUBTITLE);
        lbl.setForeground(Theme.TEXT_MAIN);

        header.add(lbl, BorderLayout.WEST);

        String[] cols = {"Unit ID", "Corps Name", "Operational Sector", "Active Members", "Contact", "Deployment Status"};
        volunteerTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(volunteerTableModel);
        table.setFont(Theme.FONT_REGULAR);
        table.setRowHeight(26);
        table.getTableHeader().setFont(Theme.FONT_BOLD);
        table.getTableHeader().setBackground(new Color(241, 245, 249));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));

        card.add(header, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    public void refreshData() {
        // 1. Alerts
        alertTableModel.setRowCount(0);
        List<VolunteerNotifier.VolunteerAlert> alerts = system.getVolunteerNotifier().getActiveAlerts();
        for (int i = alerts.size() - 1; i >= 0; i--) { // Reverse order: newest first
            VolunteerNotifier.VolunteerAlert a = alerts.get(i);
            alertTableModel.addRow(new Object[]{
                    a.getAlertId(),
                    a.getTimestamp().toLocalTime().toString().substring(0, 8),
                    a.getIncidentId(),
                    a.getSeverity().name(),
                    a.getMessage()
            });
        }

        // 2. Volunteers
        volunteerTableModel.setRowCount(0);
        for (ResponseTeam team : system.getAllResponseTeams()) {
            if (team instanceof Volunteer) {
                Volunteer v = (Volunteer) team;
                volunteerTableModel.addRow(new Object[]{
                        v.getId(),
                        v.getName(),
                        v.getCommunityDistrict(),
                        v.getVolunteerMemberCount(),
                        v.getContactNumber(),
                        v.getStatus().name()
                });
            }
        }
    }
}
