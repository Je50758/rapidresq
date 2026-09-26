package gui;

import auth.User;
import models.DonationRecord;
import models.ReliefNeed;
import patterns.DisasterManagementSystem;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Post-Disaster Donation Tracker panel.
 * Automatically generates community rehabilitation goals for resolved incidents,
 * matches relief goods, and tracks public fundraising progress.
 */
public class DonationPanel extends JPanel {
    private final User currentUser;
    private final DisasterManagementSystem system;

    private JTable needsTable;
    private DefaultTableModel needsTableModel;

    private JLabel selectedNeedTitle;
    private JLabel selectedNeedLocation;
    private JProgressBar fundingProgressBar;
    private JLabel fundingProgressLabel;
    private JLabel packageCountLabel;

    private JTextField amountField;
    private JComboBox<String> reliefItemComboBox;

    private DefaultTableModel donationsTableModel;
    private int donationCounter = 10;

    public DonationPanel(User currentUser) {
        this.currentUser = currentUser;
        this.system = DisasterManagementSystem.getInstance();

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("❤️ Post-Disaster Community Relief & Donation Tracker");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Direct crowdsourced rehabilitation aid for verified resolved disaster zones across Bangladesh.");
        subtitle.setFont(Theme.FONT_REGULAR);
        subtitle.setForeground(Theme.TEXT_MUTED);

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh Drives");
        refreshBtn.addActionListener(e -> refreshData());

        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(subtitle, BorderLayout.SOUTH);
        headerPanel.add(refreshBtn, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Center Split: Left = Active Relief Drives; Right = Donation Action & Progress Card
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createNeedsTableCard(),
                createDonationActionCard());
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(8);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createNeedsTableCard() {
        JPanel card = Theme.createCardPanel();
        card.setLayout(new BorderLayout(8, 8));

        JLabel cardTitle = new JLabel("Post-Disaster Community Rehabilitation Drives");
        cardTitle.setFont(Theme.FONT_SUBTITLE);
        cardTitle.setForeground(Theme.TEXT_MAIN);

        String[] cols = {"Need ID", "Incident Ref", "Location", "Category", "Target (BDT)", "Raised (BDT)", "Progress", "Status"};
        needsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        needsTable = new JTable(needsTableModel);
        needsTable.setFont(Theme.FONT_REGULAR);
        needsTable.setRowHeight(28);
        needsTable.getTableHeader().setFont(Theme.FONT_BOLD);
        needsTable.getTableHeader().setBackground(new Color(241, 245, 249));
        needsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        needsTable.getColumnModel().getColumn(0).setCellRenderer(center);
        needsTable.getColumnModel().getColumn(1).setCellRenderer(center);
        needsTable.getColumnModel().getColumn(4).setCellRenderer(center);
        needsTable.getColumnModel().getColumn(5).setCellRenderer(center);
        needsTable.getColumnModel().getColumn(6).setCellRenderer(center);
        needsTable.getColumnModel().getColumn(7).setCellRenderer(center);

        needsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedNeedDetails();
            }
        });

        JScrollPane scroll = new JScrollPane(needsTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));

        card.add(cardTitle, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel createDonationActionCard() {
        JPanel card = Theme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // 1. Need Overview Section
        selectedNeedTitle = new JLabel("Select a disaster relief drive");
        selectedNeedTitle.setFont(Theme.FONT_SUBTITLE);
        selectedNeedTitle.setForeground(Theme.TEXT_MAIN);
        selectedNeedTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        selectedNeedLocation = new JLabel("Location: N/A");
        selectedNeedLocation.setFont(Theme.FONT_REGULAR);
        selectedNeedLocation.setForeground(Theme.TEXT_MUTED);
        selectedNeedLocation.setAlignmentX(Component.LEFT_ALIGNMENT);

        fundingProgressBar = new JProgressBar(0, 100);
        fundingProgressBar.setStringPainted(true);
        fundingProgressBar.setFont(Theme.FONT_BOLD);
        fundingProgressBar.setForeground(Theme.SUCCESS);
        fundingProgressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        fundingProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        fundingProgressLabel = new JLabel("Funding Raised: ৳0 / ৳0 (0%)");
        fundingProgressLabel.setFont(Theme.FONT_SMALL);
        fundingProgressLabel.setForeground(Theme.TEXT_MAIN);
        fundingProgressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        packageCountLabel = new JLabel("Relief Goods Gathered: 0 / 0 packs");
        packageCountLabel.setFont(Theme.FONT_SMALL);
        packageCountLabel.setForeground(Theme.TEXT_MUTED);
        packageCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(selectedNeedTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(selectedNeedLocation);
        card.add(Box.createVerticalStrut(10));
        card.add(fundingProgressBar);
        card.add(Box.createVerticalStrut(4));
        card.add(fundingProgressLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(packageCountLabel);
        card.add(Box.createVerticalStrut(16));

        // 2. Donation Form Section
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.setOpaque(false);
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel amountLbl = new JLabel("Contribution Amount (৳ BDT):");
        amountLbl.setFont(Theme.FONT_BOLD);
        amountField = Theme.createTextField(10);
        amountField.setText("2000");

        JLabel itemLbl = new JLabel("Relief Supply Package:");
        itemLbl.setFont(Theme.FONT_BOLD);
        reliefItemComboBox = new JComboBox<>(new String[]{
                "Cash Aid",
                "Dry Food Ration Pack",
                "Clean Water & Purification Kits",
                "Burn & Trauma Medical Kit",
                "Blankets & Warm Clothing",
                "Emergency Baby Food & Care"
        });
        reliefItemComboBox.setFont(Theme.FONT_REGULAR);
        reliefItemComboBox.setBackground(Color.WHITE);

        form.add(amountLbl);
        form.add(amountField);
        form.add(itemLbl);
        form.add(reliefItemComboBox);

        card.add(form);
        card.add(Box.createVerticalStrut(12));

        JButton donateBtn = Theme.createSuccessButton("🎁 Submit Relief Contribution");
        donateBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        donateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        donateBtn.addActionListener(e -> performDonation());
        card.add(donateBtn);
        card.add(Box.createVerticalStrut(14));

        // 3. Contribution History for Selected Drive
        JLabel historyTitle = new JLabel("Recent Contributions to this Campaign");
        historyTitle.setFont(Theme.FONT_BOLD);
        historyTitle.setForeground(Theme.TEXT_MAIN);
        historyTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(historyTitle);
        card.add(Box.createVerticalStrut(6));

        String[] cols = {"Time", "Donor", "Amount (BDT)", "Relief Supply Item"};
        donationsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable donationsTable = new JTable(donationsTableModel);
        donationsTable.setFont(Theme.FONT_REGULAR);
        donationsTable.setRowHeight(24);
        donationsTable.getTableHeader().setFont(Theme.FONT_BOLD);
        donationsTable.getTableHeader().setBackground(new Color(241, 245, 249));

        JScrollPane donScroll = new JScrollPane(donationsTable);
        donScroll.setBorder(new LineBorder(Theme.BORDER, 1));
        donScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(donScroll);

        return card;
    }

    private void performDonation() {
        int selectedRow = needsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an active relief drive from the table.", "No Drive Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String needId = (String) needsTableModel.getValueAt(selectedRow, 0);
        String amountText = amountField.getText().trim();
        String item = (String) reliefItemComboBox.getSelectedItem();

        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) throw new NumberFormatException();

            String donationId = "DON-" + (++donationCounter);
            DonationRecord record = new DonationRecord(donationId, needId, currentUser.getUsername(), amount, item);

            system.donateToNeed(needId, record);

            JOptionPane.showMessageDialog(this,
                    "Thank you, @" + currentUser.getUsername() + "! Your contribution of ৳" +
                    String.format("%.2f", amount) + " (" + item + ") has been matched to drive " + needId + ".",
                    "Donation Confirmed",
                    JOptionPane.INFORMATION_MESSAGE);

            refreshData();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive donation amount.", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshData() {
        needsTableModel.setRowCount(0);
        List<ReliefNeed> list = system.getAllReliefNeeds();

        for (ReliefNeed n : list) {
            needsTableModel.addRow(new Object[]{
                    n.getNeedId(),
                    n.getIncidentId(),
                    n.getLocation(),
                    n.getDisasterType().name(),
                    String.format("৳%.0f", n.getTargetFundingBDT()),
                    String.format("৳%.0f", n.getCollectedFundingBDT()),
                    String.format("%.1f%%", n.getFundingProgressPercent()),
                    n.getStatus().name()
            });
        }

        if (needsTable.getRowCount() > 0) {
            if (needsTable.getSelectedRow() == -1) {
                needsTable.setRowSelectionInterval(0, 0);
            }
            updateSelectedNeedDetails();
        }
    }

    private void updateSelectedNeedDetails() {
        int row = needsTable.getSelectedRow();
        if (row != -1) {
            String needId = (String) needsTableModel.getValueAt(row, 0);
            ReliefNeed need = system.getReliefNeedById(needId);
            if (need != null) {
                selectedNeedTitle.setText(need.getNeedId() + ": " + need.getPrimaryReliefPackage());
                selectedNeedLocation.setText("Sector: " + need.getLocation() + " | Incident: " + need.getIncidentId());
                int pct = (int) Math.round(need.getFundingProgressPercent());
                fundingProgressBar.setValue(pct);
                fundingProgressBar.setString(pct + "% Funded");
                fundingProgressLabel.setText(String.format("Funding Raised: ৳%.0f of ৳%.0f (%.1f%%)",
                        need.getCollectedFundingBDT(), need.getTargetFundingBDT(), need.getFundingProgressPercent()));
                packageCountLabel.setText("Relief Goods Gathered: " + need.getCollectedPackagesCount() +
                        " of " + need.getTargetPackagesCount() + " target units");

                // Populate donations history
                donationsTableModel.setRowCount(0);
                List<DonationRecord> dons = need.getDonations();
                for (int i = dons.size() - 1; i >= 0; i--) {
                    DonationRecord d = dons.get(i);
                    donationsTableModel.addRow(new Object[]{
                            d.getFormattedTimestamp().substring(11, 19),
                            "@" + d.getDonorUsername(),
                            String.format("৳%.2f", d.getAmountBDT()),
                            d.getReliefItem()
                    });
                }
            }
        }
    }
}
