package admin;

import auth.Role;
import auth.User;
import auth.UserAuthenticationService;
import gui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Administrative sub-tab for managing registered users,
 * promoting/demoting roles, toggling account active status,
 * and modifying citizen trust scores.
 */
public class UserManagementSubTab extends JPanel {
    private final User currentAdmin;
    private final UserAuthenticationService authService;

    private JTable usersTable;
    private DefaultTableModel tableModel;

    public UserManagementSubTab(User currentAdmin) {
        this.currentAdmin = currentAdmin;
        this.authService = UserAuthenticationService.getInstance();

        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top Action Bar
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actionBar.setOpaque(false);

        JButton toggleStatusBtn = Theme.createButton("🔄 Toggle Active / Inactive", Theme.PRIMARY, Color.WHITE);
        toggleStatusBtn.addActionListener(e -> toggleSelectedUserStatus());

        JButton changeRoleBtn = Theme.createButton("🏷️ Change User Role", Theme.ACCENT, Color.WHITE);
        changeRoleBtn.addActionListener(e -> changeSelectedUserRole());

        JButton editTrustBtn = Theme.createButton("⭐ Adjust Trust Score", Theme.WARNING, Color.WHITE);
        editTrustBtn.addActionListener(e -> editSelectedUserTrust());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshUserList());

        actionBar.add(toggleStatusBtn);
        actionBar.add(changeRoleBtn);
        actionBar.add(editTrustBtn);
        actionBar.add(refreshBtn);

        add(actionBar, BorderLayout.NORTH);

        // Center Table Card
        JPanel card = Theme.createCardPanel();
        card.setLayout(new BorderLayout(8, 8));

        JLabel title = new JLabel("Registered System Users & Credential Authority");
        title.setFont(Theme.FONT_SUBTITLE);
        title.setForeground(Theme.TEXT_MAIN);

        String[] cols = {"Username", "System Role", "Trust Score (%)", "Account Status", "Privilege Scope"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        usersTable = new JTable(tableModel);
        usersTable.setFont(Theme.FONT_REGULAR);
        usersTable.setRowHeight(28);
        usersTable.getTableHeader().setFont(Theme.FONT_BOLD);
        usersTable.getTableHeader().setBackground(new Color(241, 245, 249));
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        usersTable.getColumnModel().getColumn(1).setCellRenderer(center);
        usersTable.getColumnModel().getColumn(2).setCellRenderer(center);
        usersTable.getColumnModel().getColumn(3).setCellRenderer(center);

        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));

        card.add(title, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        refreshUserList();
    }

    public void refreshUserList() {
        tableModel.setRowCount(0);
        List<User> users = authService.getAllUsers();
        for (User u : users) {
            String statusStr = u.isActive() ? "ACTIVE" : "DEACTIVATED";
            String scope = u.getRole() == Role.ADMIN ? "Full Administrative Control" :
                    u.getRole() == Role.RESPONSE_TEAM ? "Field Unit Dispatch" :
                    u.getRole() == Role.VOLUNTEER ? "Community First Response" : "Public Crowdsource";

            tableModel.addRow(new Object[]{
                    u.getUsername(),
                    u.getRole().getDisplayName(),
                    u.getTrustScore() + "%",
                    statusStr,
                    scope
            });
        }
    }

    private void toggleSelectedUserStatus() {
        int row = usersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to toggle status.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = (String) tableModel.getValueAt(row, 0);
        if (username.equalsIgnoreCase("admin")) {
            JOptionPane.showMessageDialog(this, "Root administrator 'admin' cannot be deactivated.", "Protected Account", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean success = authService.toggleUserStatus(username);
        if (success) {
            User u = authService.getUser(username);
            JOptionPane.showMessageDialog(this,
                    "User @" + username + " is now " + (u.isActive() ? "ACTIVE." : "DEACTIVATED."),
                    "Status Updated",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshUserList();
        }
    }

    private void changeSelectedUserRole() {
        int row = usersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to change role.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = (String) tableModel.getValueAt(row, 0);
        User u = authService.getUser(username);
        if (u == null) return;

        Role selectedRole = (Role) JOptionPane.showInputDialog(
                this,
                "Select new authorization role for @" + username + ":",
                "Change Role",
                JOptionPane.QUESTION_MESSAGE,
                null,
                Role.values(),
                u.getRole()
        );

        if (selectedRole != null && selectedRole != u.getRole()) {
            authService.updateUserRole(username, selectedRole);
            JOptionPane.showMessageDialog(this,
                    "Updated @" + username + " to role: " + selectedRole.getDisplayName(),
                    "Role Changed",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshUserList();
        }
    }

    private void editSelectedUserTrust() {
        int row = usersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to modify trust score.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = (String) tableModel.getValueAt(row, 0);
        User u = authService.getUser(username);
        if (u == null) return;

        String input = JOptionPane.showInputDialog(
                this,
                "Enter new Trust Score (0 - 100) for @" + username + ":",
                u.getTrustScore()
        );

        if (input != null && !input.trim().isEmpty()) {
            try {
                int newScore = Integer.parseInt(input.trim());
                if (newScore < 0 || newScore > 100) {
                    throw new NumberFormatException();
                }
                authService.updateTrustScore(username, newScore);
                JOptionPane.showMessageDialog(this,
                        "Trust score for @" + username + " updated to " + newScore + "%",
                        "Trust Score Updated",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshUserList();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Trust score must be an integer between 0 and 100.", "Invalid Value", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
