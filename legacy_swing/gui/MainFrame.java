package gui;

import auth.Role;
import auth.User;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Main application window displaying role-filtered navigation tabs,
 * current user profile badge, trust score indicator, and top action bar.
 * Integrates Phase 4 Unique Features: 999 Escalation Stream and Post-Disaster Donation Tracker.
 */
public class MainFrame extends JFrame {
    private final User currentUser;
    private JTabbedPane mainTabbedPane;

    public MainFrame(User currentUser) {
        super("RapidResQ — Rapid Disaster Response System");
        this.currentUser = currentUser;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 780);
        setMinimumSize(new Dimension(950, 640));
        setLocationRelativeTo(null);

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(Theme.BACKGROUND);

        // 1. Top Navigation Bar (User info, Role Badge, Logout)
        rootPanel.add(createTopNavBar(), BorderLayout.NORTH);

        // 2. Role-Based Dynamic Tabbed Pane
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(Theme.FONT_BOLD);
        mainTabbedPane.setBackground(Color.WHITE);
        setupRoleTabs();

        rootPanel.add(mainTabbedPane, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        rootPanel.add(createBottomStatusBar(), BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    private JPanel createTopNavBar() {
        JPanel navBar = new JPanel(new BorderLayout(16, 0));
        navBar.setBackground(Theme.PRIMARY);
        navBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Brand / Title
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandPanel.setOpaque(false);

        JLabel brandIcon = new JLabel("🚨");
        brandIcon.setFont(new Font("Segoe UI", Font.PLAIN, 22));

        JLabel brandText = new JLabel("RapidResQ Bangladesh");
        brandText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brandText.setForeground(Color.WHITE);

        JLabel brandDesc = new JLabel("| Rapid Response & Relief Coordination");
        brandDesc.setFont(Theme.FONT_SMALL);
        brandDesc.setForeground(new Color(148, 163, 184)); // Slate 400

        brandPanel.add(brandIcon);
        brandPanel.add(brandText);
        brandPanel.add(brandDesc);

        // User Info & Actions
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        userPanel.setOpaque(false);

        JLabel userLabel = new JLabel("Logged in as: @" + currentUser.getUsername());
        userLabel.setFont(Theme.FONT_REGULAR);
        userLabel.setForeground(Color.WHITE);

        // Role badge
        JLabel roleBadge = Theme.createRoleBadge(currentUser.getRole());

        // Trust Score indicator
        JLabel trustBadge = new JLabel(" Trust: " + currentUser.getTrustScore() + "% ");
        trustBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        trustBadge.setOpaque(true);
        trustBadge.setBackground(new Color(241, 245, 249));
        trustBadge.setForeground(Theme.TEXT_MAIN);
        trustBadge.setBorder(new LineBorder(Theme.BORDER, 1, true));

        // Logout Button
        JButton logoutBtn = Theme.createButton("Logout", new Color(239, 68, 68), Color.WHITE);
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        logoutBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        logoutBtn.addActionListener(e -> performLogout());

        userPanel.add(userLabel);
        userPanel.add(roleBadge);
        userPanel.add(trustBadge);
        userPanel.add(logoutBtn);

        navBar.add(brandPanel, BorderLayout.WEST);
        navBar.add(userPanel, BorderLayout.EAST);

        return navBar;
    }

    private void setupRoleTabs() {
        Role role = currentUser.getRole();

        // 1. Dashboard (All roles)
        mainTabbedPane.addTab("📊 Incident Dashboard", new DashboardPanel(currentUser));

        // 2. Report Incident (Citizen, Volunteer, Admin)
        if (role == Role.CITIZEN || role == Role.VOLUNTEER || role == Role.ADMIN) {
            mainTabbedPane.addTab("📢 Report Incident", new ReportIncidentPanel(currentUser));
        }

        // 3. Volunteer Network (Volunteer, Admin)
        if (role == Role.VOLUNTEER || role == Role.ADMIN) {
            mainTabbedPane.addTab("🤝 Volunteer Network", new VolunteerNetworkPanel(currentUser));
        }

        // 4. Teams & Resources (Response Team, Admin)
        if (role == Role.RESPONSE_TEAM || role == Role.ADMIN) {
            mainTabbedPane.addTab("🚒 Teams & Resources", new TeamsPanel(currentUser));
        }

        // 5. Emergency Escalation Console (Volunteer, Response Team, Admin)
        if (role == Role.VOLUNTEER || role == Role.RESPONSE_TEAM || role == Role.ADMIN) {
            mainTabbedPane.addTab("🚨 999 Escalations", new EscalationLogPanel(currentUser));
        }

        // 6. Post-Disaster Donation Tracker (All roles can contribute & view)
        mainTabbedPane.addTab("❤️ Relief & Donations", new DonationPanel(currentUser));

        // 7. Admin Panel (Admin only)
        if (role == Role.ADMIN) {
            mainTabbedPane.addTab("🛡️ Admin Panel", new AdminPanel(currentUser));
        }
    }

    private JPanel createBottomStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(Color.WHITE);
        status.setBorder(new CompoundBorder(new LineBorder(Theme.BORDER, 1), new EmptyBorder(6, 16, 6, 16)));

        JLabel left = new JLabel("System Status: Online | Trust-Weighted Priority Engine Active");
        left.setFont(Theme.FONT_SMALL);
        left.setForeground(Theme.TEXT_MUTED);

        JLabel right = new JLabel("National Emergency Coordination & Crowdsourced Relief Framework (Bangladesh)");
        right.setFont(Theme.FONT_SMALL);
        right.setForeground(Theme.TEXT_MUTED);

        status.add(left, BorderLayout.WEST);
        status.add(right, BorderLayout.EAST);

        return status;
    }

    private void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to sign out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new AuthFrame().setVisible(true));
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
