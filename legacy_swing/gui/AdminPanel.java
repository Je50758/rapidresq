package gui;

import admin.AnalyticsSubTab;
import admin.IncidentOverrideSubTab;
import admin.UserManagementSubTab;
import auth.User;
import patterns.DisasterManagementSystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Administrative Command Center hosting the 3 core management sub-tabs:
 * 1. User Management (Roles, Activation, Trust Scoring)
 * 2. Incident Override (Priority fine-tuning, Team reassignment, Force resolve)
 * 3. Analytics Dashboard (Custom bar chart, Hotspots table, Response times)
 */
public class AdminPanel extends JPanel {
    private final User currentUser;
    private final DisasterManagementSystem system;
    private final JTabbedPane adminSubTabs;

    private final UserManagementSubTab userManagementSubTab;
    private final IncidentOverrideSubTab incidentOverrideSubTab;
    private final AnalyticsSubTab analyticsSubTab;

    public AdminPanel(User currentUser) {
        this.currentUser = currentUser;
        this.system = DisasterManagementSystem.getInstance();

        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("🛡️ Central Administrative Command Center");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Privileged administrative tools, user credential authority, manual dispatch overrides, and hot-spot analytics.");
        subtitle.setFont(Theme.FONT_REGULAR);
        subtitle.setForeground(Theme.TEXT_MUTED);

        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(subtitle, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // Sub-tabs
        adminSubTabs = new JTabbedPane();
        adminSubTabs.setFont(Theme.FONT_BOLD);
        adminSubTabs.setBackground(Color.WHITE);

        userManagementSubTab = new UserManagementSubTab(currentUser);
        incidentOverrideSubTab = new IncidentOverrideSubTab();
        analyticsSubTab = new AnalyticsSubTab();

        adminSubTabs.addTab("👤 User Management", userManagementSubTab);
        adminSubTabs.addTab("⚡ Incident Override", incidentOverrideSubTab);
        adminSubTabs.addTab("📊 Analytics & Hotspots", analyticsSubTab);

        // Refresh sub-tab contents when tab switched
        adminSubTabs.addChangeListener(e -> {
            int idx = adminSubTabs.getSelectedIndex();
            if (idx == 0) userManagementSubTab.refreshUserList();
            else if (idx == 1) incidentOverrideSubTab.refreshIncidentList();
            else if (idx == 2) analyticsSubTab.refreshAnalytics();
        });

        add(adminSubTabs, BorderLayout.CENTER);
    }

    public void refreshAll() {
        userManagementSubTab.refreshUserList();
        incidentOverrideSubTab.refreshIncidentList();
        analyticsSubTab.refreshAnalytics();
    }
}
