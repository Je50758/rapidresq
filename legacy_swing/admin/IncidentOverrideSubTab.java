package admin;

import exceptions.InvalidIncidentException;
import gui.Theme;
import models.Incident;
import models.IncidentStatus;
import models.ResponseTeam;
import patterns.DisasterManagementSystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Administrative sub-tab for manual dispatch override,
 * priority fine-tuning, emergency team reassignment, and forced resolution.
 */
public class IncidentOverrideSubTab extends JPanel {
    private final DisasterManagementSystem system;

    private JTable incidentTable;
    private DefaultTableModel tableModel;

    public IncidentOverrideSubTab() {
        this.system = DisasterManagementSystem.getInstance();

        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top Action Bar
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actionBar.setOpaque(false);

        JButton overridePriorityBtn = Theme.createButton("⚡ Override Priority", Theme.WARNING, Color.WHITE);
        overridePriorityBtn.addActionListener(e -> overrideSelectedIncidentPriority());

        JButton reassignTeamBtn = Theme.createButton("🔄 Reassign Response Team", Theme.ACCENT, Color.WHITE);
        reassignTeamBtn.addActionListener(e -> reassignSelectedIncidentTeam());

        JButton forceResolveBtn = Theme.createButton("✅ Force Resolve & Clear", Theme.SUCCESS, Color.WHITE);
        forceResolveBtn.addActionListener(e -> forceResolveSelectedIncident());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshIncidentList());

        actionBar.add(overridePriorityBtn);
        actionBar.add(reassignTeamBtn);
        actionBar.add(forceResolveBtn);
        actionBar.add(refreshBtn);

        add(actionBar, BorderLayout.NORTH);

        // Center Table Card
        JPanel card = Theme.createCardPanel();
        card.setLayout(new BorderLayout(8, 8));

        JLabel title = new JLabel("Live Incident Override & Operational Dispatch Control");
        title.setFont(Theme.FONT_SUBTITLE);
        title.setForeground(Theme.TEXT_MAIN);

        String[] cols = {"ID", "Disaster Type", "Location", "Severity", "Priority Score", "Status", "Assigned Unit", "Injured", "Reports"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        incidentTable = new JTable(tableModel);
        incidentTable.setFont(Theme.FONT_REGULAR);
        incidentTable.setRowHeight(28);
        incidentTable.getTableHeader().setFont(Theme.FONT_BOLD);
        incidentTable.getTableHeader().setBackground(new Color(241, 245, 249));
        incidentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        incidentTable.getColumnModel().getColumn(0).setCellRenderer(center);
        incidentTable.getColumnModel().getColumn(4).setCellRenderer(center);
        incidentTable.getColumnModel().getColumn(5).setCellRenderer(center);
        incidentTable.getColumnModel().getColumn(7).setCellRenderer(center);
        incidentTable.getColumnModel().getColumn(8).setCellRenderer(center);

        JScrollPane scroll = new JScrollPane(incidentTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));

        card.add(title, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        refreshIncidentList();
    }

    public void refreshIncidentList() {
        tableModel.setRowCount(0);
        List<Incident> list = new ArrayList<>(system.getAllIncidents());
        list.sort((a, b) -> Double.compare(b.getPriority(), a.getPriority()));

        for (Incident inc : list) {
            tableModel.addRow(new Object[]{
                    inc.getId(),
                    inc.getDisasterType().name(),
                    inc.getLocation(),
                    inc.getSeverity().name(),
                    String.format("%.1f", inc.getPriority()),
                    inc.getStatus().name(),
                    inc.getAssignedTeamId() != null ? inc.getAssignedTeamId() : "Unassigned",
                    inc.getInjuredCount(),
                    inc.getReportCount()
            });
        }
    }

    private void overrideSelectedIncidentPriority() {
        int row = incidentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an incident to override priority.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) tableModel.getValueAt(row, 0);
        Incident inc = system.getIncidentById(id);
        if (inc == null) return;

        String input = JOptionPane.showInputDialog(
                this,
                "Enter new manual Priority Score for " + id + " (" + inc.getDisasterType() + "):\n(Current: " + inc.getPriority() + ")",
                inc.getPriority()
        );

        if (input != null && !input.trim().isEmpty()) {
            try {
                double newPrio = Double.parseDouble(input.trim());
                if (newPrio < 0) throw new NumberFormatException();

                inc.setPriority(newPrio);
                JOptionPane.showMessageDialog(this,
                        "Priority for " + id + " manually updated to " + newPrio + ".\nPriority triage queue re-ordered.",
                        "Priority Overridden",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshIncidentList();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive decimal number.", "Invalid Number", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void reassignSelectedIncidentTeam() {
        int row = incidentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an incident to reassign team.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) tableModel.getValueAt(row, 0);
        Incident inc = system.getIncidentById(id);
        if (inc == null) return;

        if (inc.getStatus() == IncidentStatus.RESOLVED) {
            JOptionPane.showMessageDialog(this, "Cannot assign team: Incident is already RESOLVED.", "Action Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Collect available teams
        List<ResponseTeam> candidates = new ArrayList<>();
        for (ResponseTeam team : system.getAllResponseTeams()) {
            if (team.isAvailable() && team.canHandle(inc)) {
                candidates.add(team);
            }
        }

        if (candidates.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No currently available teams are compatible with disaster type: " + inc.getDisasterType(),
                    "No Units Available",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        ResponseTeam selectedTeam = (ResponseTeam) JOptionPane.showInputDialog(
                this,
                "Select replacement Response Team for " + id + ":",
                "Reassign Response Team",
                JOptionPane.QUESTION_MESSAGE,
                null,
                candidates.toArray(new ResponseTeam[0]),
                candidates.get(0)
        );

        if (selectedTeam != null) {
            // Release existing team if assigned
            if (inc.getAssignedTeamId() != null) {
                ResponseTeam currentTeam = system.getTeamById(inc.getAssignedTeamId());
                if (currentTeam != null) {
                    currentTeam.release();
                }
            }

            // Assign new team
            selectedTeam.assignToIncident(id);
            inc.setAssignedTeamId(selectedTeam.getId());
            inc.setStatus(IncidentStatus.IN_PROGRESS);

            JOptionPane.showMessageDialog(this,
                    "Reassigned incident " + id + " to " + selectedTeam.getName() + " (" + selectedTeam.getId() + ").",
                    "Team Reassigned",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshIncidentList();
        }
    }

    private void forceResolveSelectedIncident() {
        int row = incidentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an incident to force resolve.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) tableModel.getValueAt(row, 0);
        Incident inc = system.getIncidentById(id);
        if (inc == null) return;

        if (inc.getStatus() == IncidentStatus.RESOLVED) {
            JOptionPane.showMessageDialog(this, "Incident " + id + " is already marked RESOLVED.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Force resolve incident " + id + "?\nThis will release any dispatched units back to AVAILABLE.",
                "Confirm Force Resolution",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                system.resolveIncident(id);
                JOptionPane.showMessageDialog(this, "Incident " + id + " has been forcefully marked RESOLVED.", "Resolution Confirmed", JOptionPane.INFORMATION_MESSAGE);
                refreshIncidentList();
            } catch (InvalidIncidentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Resolution Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
