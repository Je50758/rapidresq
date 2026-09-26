package gui;

import auth.User;
import exceptions.InvalidIncidentException;
import models.*;
import patterns.DisasterManagementSystem;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Modern form allowing citizens and volunteers to file structured emergency reports.
 * Dynamically switches parameter inputs based on disaster category polymorphism.
 */
public class ReportIncidentPanel extends JPanel {
    private final User currentUser;
    private final DisasterManagementSystem system;

    private JComboBox<DisasterType> typeComboBox;
    private JTextField locationField;
    private JSpinner injuredSpinner;
    private JComboBox<Severity> severityComboBox;
    private JTextArea descriptionArea;

    // Fire specific
    private JSpinner fireAlarmSpinner;
    private JCheckBox chemicalHazardCheck;

    // Flood specific
    private JTextField waterLevelField;
    private JSpinner strandedSpinner;

    // Accident specific
    private JSpinner vehiclesSpinner;
    private JCheckBox highwayTrainCheck;

    // Earthquake specific
    private JTextField magnitudeField;
    private JSpinner collapsedBuildingsSpinner;

    private JPanel dynamicCardPanel;
    private CardLayout dynamicCardLayout;

    public ReportIncidentPanel(User currentUser) {
        this.currentUser = currentUser;
        this.system = DisasterManagementSystem.getInstance();

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(20, 40, 20, 40));

        JPanel container = Theme.createCardPanel();
        container.setLayout(new BorderLayout(16, 16));

        // Form Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("📢 Report an Emergency Incident");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Crowdsourced reporting triggers priority-queued response teams and volunteer alerts.");
        subtitle.setFont(Theme.FONT_REGULAR);
        subtitle.setForeground(Theme.TEXT_MUTED);

        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(subtitle, BorderLayout.SOUTH);

        // Form Fields (GridBagLayout)
        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Disaster Type
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formGrid.add(createFieldLabel("Disaster Type"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.7;
        typeComboBox = new JComboBox<>(DisasterType.values());
        typeComboBox.setFont(Theme.FONT_REGULAR);
        typeComboBox.addActionListener(e -> onTypeChanged());
        formGrid.add(typeComboBox, gbc);

        // Location
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        formGrid.add(createFieldLabel("Location (District / Area)"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.7;
        locationField = Theme.createTextField(25);
        formGrid.add(locationField, gbc);

        // Injured Count
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        formGrid.add(createFieldLabel("Estimated Injured / Casualties"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.7;
        injuredSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        injuredSpinner.setFont(Theme.FONT_REGULAR);
        formGrid.add(injuredSpinner, gbc);

        // Severity
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        formGrid.add(createFieldLabel("Preliminary Severity"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.7;
        severityComboBox = new JComboBox<>(Severity.values());
        severityComboBox.setFont(Theme.FONT_REGULAR);
        formGrid.add(severityComboBox, gbc);

        // Dynamic Sub-panel for Type-Specific Attributes
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        formGrid.add(createFieldLabel("Special Parameters"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0.7;

        dynamicCardLayout = new CardLayout();
        dynamicCardPanel = new JPanel(dynamicCardLayout);
        dynamicCardPanel.setOpaque(false);
        initDynamicSubPanels();
        formGrid.add(dynamicCardPanel, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3;
        formGrid.add(createFieldLabel("Description / Observations"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 0.7;
        descriptionArea = new JTextArea(3, 25);
        descriptionArea.setFont(Theme.FONT_REGULAR);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(new LineBorder(Theme.BORDER, 1));
        formGrid.add(descScroll, gbc);

        // Submit Button Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setOpaque(false);

        JButton submitBtn = Theme.createPrimaryButton("🚨 Submit Emergency Report");
        submitBtn.setFont(Theme.FONT_SUBTITLE);
        submitBtn.addActionListener(e -> submitReport());

        actionPanel.add(submitBtn);

        container.add(titlePanel, BorderLayout.NORTH);
        container.add(formGrid, BorderLayout.CENTER);
        container.add(actionPanel, BorderLayout.SOUTH);

        add(container, BorderLayout.CENTER);
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setForeground(Theme.TEXT_MAIN);
        return lbl;
    }

    private void initDynamicSubPanels() {
        // 1. Fire
        JPanel firePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        firePanel.setOpaque(false);
        firePanel.add(new JLabel("Alarm Level (1-5):"));
        fireAlarmSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 5, 1));
        firePanel.add(fireAlarmSpinner);
        chemicalHazardCheck = new JCheckBox("Chemical / Gas Cylinder Threat");
        chemicalHazardCheck.setOpaque(false);
        firePanel.add(chemicalHazardCheck);
        dynamicCardPanel.add(firePanel, DisasterType.FIRE.name());

        // 2. Flood
        JPanel floodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        floodPanel.setOpaque(false);
        floodPanel.add(new JLabel("Water Level (m):"));
        waterLevelField = Theme.createTextField(5);
        waterLevelField.setText("1.5");
        floodPanel.add(waterLevelField);
        floodPanel.add(new JLabel("Stranded Persons:"));
        strandedSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 5000, 5));
        floodPanel.add(strandedSpinner);
        dynamicCardPanel.add(floodPanel, DisasterType.FLOOD.name());

        // 3. Accident
        JPanel accidentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        accidentPanel.setOpaque(false);
        accidentPanel.add(new JLabel("Vehicles Involved:"));
        vehiclesSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        accidentPanel.add(vehiclesSpinner);
        highwayTrainCheck = new JCheckBox("Highway / Rail Collision");
        highwayTrainCheck.setOpaque(false);
        accidentPanel.add(highwayTrainCheck);
        dynamicCardPanel.add(accidentPanel, DisasterType.ACCIDENT.name());

        // 4. Earthquake
        JPanel eqPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        eqPanel.setOpaque(false);
        eqPanel.add(new JLabel("Richter Magnitude:"));
        magnitudeField = Theme.createTextField(5);
        magnitudeField.setText("4.5");
        eqPanel.add(magnitudeField);
        eqPanel.add(new JLabel("Collapsed Structures:"));
        collapsedBuildingsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        eqPanel.add(collapsedBuildingsSpinner);
        dynamicCardPanel.add(eqPanel, DisasterType.EARTHQUAKE.name());
    }

    private void onTypeChanged() {
        DisasterType type = (DisasterType) typeComboBox.getSelectedItem();
        if (type != null) {
            dynamicCardLayout.show(dynamicCardPanel, type.name());
        }
    }

    private void submitReport() {
        String location = locationField.getText().trim();
        if (location.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an incident location.", "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DisasterType type = (DisasterType) typeComboBox.getSelectedItem();
        int injured = (Integer) injuredSpinner.getValue();
        Severity severity = (Severity) severityComboBox.getSelectedItem();
        String desc = descriptionArea.getText().trim();
        String incidentId = system.generateNextIncidentId();

        Incident incident;
        try {
            switch (type) {
                case FIRE:
                    int alarm = (Integer) fireAlarmSpinner.getValue();
                    boolean chem = chemicalHazardCheck.isSelected();
                    incident = new FireIncident(incidentId, location, injured, severity,
                            currentUser.getUsername(), currentUser.getTrustScore(), desc, alarm, chem);
                    break;
                case FLOOD:
                    double water = Double.parseDouble(waterLevelField.getText().trim());
                    int stranded = (Integer) strandedSpinner.getValue();
                    incident = new FloodIncident(incidentId, location, injured, severity,
                            currentUser.getUsername(), currentUser.getTrustScore(), desc, water, stranded);
                    break;
                case ACCIDENT:
                    int vehicles = (Integer) vehiclesSpinner.getValue();
                    boolean highway = highwayTrainCheck.isSelected();
                    incident = new AccidentIncident(incidentId, location, injured, severity,
                            currentUser.getUsername(), currentUser.getTrustScore(), desc, vehicles, highway);
                    break;
                case EARTHQUAKE:
                    double mag = Double.parseDouble(magnitudeField.getText().trim());
                    int collapsed = (Integer) collapsedBuildingsSpinner.getValue();
                    incident = new EarthquakeIncident(incidentId, location, injured, severity,
                            currentUser.getUsername(), currentUser.getTrustScore(), desc, mag, collapsed);
                    break;
                default:
                    throw new InvalidIncidentException("Unsupported disaster type.");
            }

            system.reportIncident(incident);

            JOptionPane.showMessageDialog(this,
                    "Emergency report successfully lodged!\n" +
                    "Incident ID: " + incident.getId() + "\n" +
                    "Calculated Priority Score: " + incident.getPriority() + "\n" +
                    "Status: " + incident.getStatus().getDisplayName() + "\n" +
                    "Relevant volunteer responders and emergency units have been notified.",
                    "Report Dispatched",
                    JOptionPane.INFORMATION_MESSAGE);

            // Reset inputs
            locationField.setText("");
            descriptionArea.setText("");
            injuredSpinner.setValue(0);

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric figures for magnitude/water level.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (InvalidIncidentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Reporting Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
