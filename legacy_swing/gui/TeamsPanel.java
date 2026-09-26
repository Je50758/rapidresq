package gui;

import auth.User;
import exceptions.InvalidIncidentException;
import exceptions.NoTeamAvailableException;
import models.Incident;
import models.Resource;
import models.ResponseTeam;
import patterns.DisasterManagementSystem;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Teams & Resources Panel displaying specialized emergency units
 * and their composed equipment inventory.
 */
public class TeamsPanel extends JPanel {
    private final User currentUser;
    private final DisasterManagementSystem system;

    private DefaultTableModel teamsTableModel;
    private DefaultTableModel resourcesTableModel;
    private JTable teamsTable;

    public TeamsPanel(User currentUser) {
        this.currentUser = currentUser;
        this.system = DisasterManagementSystem.getInstance();

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("🚒 Response Teams & Resource Inventory");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Specialized national emergency units, vehicle fleets, and trauma apparatus.");
        subtitle.setFont(Theme.FONT_REGULAR);
        subtitle.setForeground(Theme.TEXT_MUTED);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton assignTopBtn = Theme.createPrimaryButton("⚡ Assign Available Team to Top Incident");
        assignTopBtn.addActionListener(e -> dispatchToTopIncident());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshData());

        btnPanel.add(assignTopBtn);
        btnPanel.add(refreshBtn);

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(btnPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Center Split: Teams Table on Top, Resources (Composition) on Bottom
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                createTeamsCard(),
                createResourcesCard());
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(8);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createTeamsCard() {
        JPanel card = Theme.createCardPanel();
        card.setLayout(new BorderLayout(8, 8));

        JLabel lbl = new JLabel("Registered Emergency Units");
        lbl.setFont(Theme.FONT_SUBTITLE);
        lbl.setForeground(Theme.TEXT_MAIN);

        String[] cols = {"Team ID", "Name", "Specialization", "Base Station", "Contact", "Status", "Assigned Incident"};
        teamsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        teamsTable = new JTable(teamsTableModel);
        teamsTable.setFont(Theme.FONT_REGULAR);
        teamsTable.setRowHeight(28);
        teamsTable.getTableHeader().setFont(Theme.FONT_BOLD);
        teamsTable.getTableHeader().setBackground(new Color(241, 245, 249));
        teamsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        teamsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateResourcesTable();
            }
        });

        JScrollPane scroll = new JScrollPane(teamsTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));

        card.add(lbl, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel createResourcesCard() {
        JPanel card = Theme.createCardPanel();
        card.setLayout(new BorderLayout(8, 8));

        JLabel lbl = new JLabel("Composed Resources & Equipment Inventory (OOP Composition)");
        lbl.setFont(Theme.FONT_SUBTITLE);
        lbl.setForeground(Theme.TEXT_MAIN);

        String[] cols = {"Resource ID", "Equipment / Vehicle Name", "Type", "Quantity", "Operational Condition"};
        resourcesTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable resTable = new JTable(resourcesTableModel);
        resTable.setFont(Theme.FONT_REGULAR);
        resTable.setRowHeight(26);
        resTable.getTableHeader().setFont(Theme.FONT_BOLD);
        resTable.getTableHeader().setBackground(new Color(241, 245, 249));

        JScrollPane scroll = new JScrollPane(resTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));

        card.add(lbl, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private void dispatchToTopIncident() {
        Incident top = system.peekHighestUrgencyIncident();
        if (top == null) {
            JOptionPane.showMessageDialog(this, "No active unresolved incidents in priority queue.", "Queue Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            ResponseTeam team = system.assignTeam(top.getId());
            JOptionPane.showMessageDialog(this,
                    "Dispatched " + team.getName() + " to Incident " + top.getId() + " (" + top.getDisasterType() + ")!\n" +
                    "Incident priority: " + top.getPriority(),
                    "Team Dispatched",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (NoTeamAvailableException | InvalidIncidentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Dispatch Failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void refreshData() {
        teamsTableModel.setRowCount(0);
        for (ResponseTeam team : system.getAllResponseTeams()) {
            teamsTableModel.addRow(new Object[]{
                    team.getId(),
                    team.getName(),
                    team.getSpecialization(),
                    team.getBaseStation(),
                    team.getContactNumber(),
                    team.getStatus().name(),
                    team.getCurrentIncidentId() != null ? team.getCurrentIncidentId() : "None"
            });
        }

        if (teamsTable.getRowCount() > 0) {
            teamsTable.setRowSelectionInterval(0, 0);
            updateResourcesTable();
        }
    }

    private void updateResourcesTable() {
        resourcesTableModel.setRowCount(0);
        int row = teamsTable.getSelectedRow();
        if (row != -1) {
            String teamId = (String) teamsTableModel.getValueAt(row, 0);
            ResponseTeam team = system.getTeamById(teamId);
            if (team != null) {
                for (Resource r : team.getResources()) {
                    resourcesTableModel.addRow(new Object[]{
                            r.getId(),
                            r.getName(),
                            r.getType().name(),
                            r.getQuantity(),
                            r.getOperationalCondition()
                    });
                }
            }
        }
    }
}
