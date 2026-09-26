package gui;

import auth.User;
import auth.UserAuthenticationService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Modern card-style login panel with preloaded demo credentials quick-picker.
 */
public class LoginPanel extends JPanel {
    private final AuthFrame parentFrame;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel errorLabel;

    public LoginPanel(AuthFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);

        // Center Card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.CARD_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(32, 40, 32, 40)
        ));
        card.setPreferredSize(new Dimension(420, 560));

        // Header Title
        JLabel logoLabel = new JLabel("🚨 RapidResQ BANGLADESH");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoLabel.setForeground(Theme.ACCENT);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Rapid Disaster Response System");
        subtitleLabel.setFont(Theme.FONT_SUBTITLE);
        subtitleLabel.setForeground(Theme.TEXT_MAIN);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("Sign in to access your portal");
        descLabel.setFont(Theme.FONT_SMALL);
        descLabel.setForeground(Theme.TEXT_MUTED);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Input Fields
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(Theme.FONT_BOLD);
        userLabel.setForeground(Theme.TEXT_MAIN);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = Theme.createTextField(20);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(Theme.FONT_BOLD);
        passLabel.setForeground(Theme.TEXT_MAIN);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = Theme.createPasswordField(20);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Error message
        errorLabel = new JLabel(" ");
        errorLabel.setFont(Theme.FONT_SMALL);
        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Login Button
        JButton loginButton = Theme.createPrimaryButton("Sign In to Portal");
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());

        // Press Enter to submit
        passwordField.addActionListener(e -> performLogin());
        usernameField.addActionListener(e -> performLogin());

        // Toggle to Register
        JButton registerToggleBtn = new JButton("Don't have an account? Register here");
        registerToggleBtn.setFont(Theme.FONT_SMALL);
        registerToggleBtn.setForeground(Theme.ACCENT);
        registerToggleBtn.setBorderPainted(false);
        registerToggleBtn.setContentAreaFilled(false);
        registerToggleBtn.setFocusPainted(false);
        registerToggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerToggleBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerToggleBtn.addActionListener(e -> parentFrame.showRegisterPanel());

        // Demo Accounts Quick Picker Card
        JPanel demoBox = new JPanel();
        demoBox.setLayout(new BoxLayout(demoBox, BoxLayout.Y_AXIS));
        demoBox.setBackground(new Color(241, 245, 249));
        demoBox.setBorder(new CompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        demoBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        demoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel demoTitle = new JLabel("⚡ Quick Demo Credentials (Click to Fill):");
        demoTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        demoTitle.setForeground(Theme.TEXT_MAIN);
        demoBox.add(demoTitle);
        demoBox.add(Box.createVerticalStrut(6));

        JPanel demoBtns = new JPanel(new GridLayout(2, 2, 6, 6));
        demoBtns.setOpaque(false);

        demoBtns.add(createQuickFillBtn("Admin (admin)", "admin", "admin123"));
        demoBtns.add(createQuickFillBtn("Citizen (citizen1)", "citizen1", "pass123"));
        demoBtns.add(createQuickFillBtn("Volunteer (vol1)", "volunteer1", "pass123"));
        demoBtns.add(createQuickFillBtn("Team (team1)", "team1", "pass123"));
        demoBox.add(demoBtns);

        // Assembly
        card.add(logoLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(descLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(userLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(10));
        card.add(passLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(8));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(6));
        card.add(registerToggleBtn);
        card.add(Box.createVerticalStrut(14));
        card.add(demoBox);

        add(card);
    }

    private JButton createQuickFillBtn(String title, String user, String pass) {
        JButton btn = new JButton(title);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btn.setBackground(Color.WHITE);
        btn.setForeground(Theme.TEXT_MAIN);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            usernameField.setText(user);
            passwordField.setText(pass);
            errorLabel.setText(" ");
        });
        return btn;
    }

    private void performLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try {
            User user = UserAuthenticationService.getInstance().login(username, password);
            errorLabel.setText(" ");
            parentFrame.onLoginSuccessful(user);
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
        }
    }
}
