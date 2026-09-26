package gui;

import auth.Role;
import auth.User;
import auth.UserAuthenticationService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Modern card-style registration panel for new citizens and volunteers.
 */
public class RegisterPanel extends JPanel {
    private final AuthFrame parentFrame;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final JComboBox<Role> roleComboBox;
    private final JLabel errorLabel;

    public RegisterPanel(AuthFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);

        // Center Card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.CARD_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(28, 40, 28, 40)
        ));
        card.setPreferredSize(new Dimension(420, 560));

        JLabel titleLabel = new JLabel("Create Community Account");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.TEXT_MAIN);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("Join as a citizen reporter or local volunteer");
        descLabel.setFont(Theme.FONT_SMALL);
        descLabel.setForeground(Theme.TEXT_MUTED);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Username
        JLabel userLabel = new JLabel("Choose Username");
        userLabel.setFont(Theme.FONT_BOLD);
        userLabel.setForeground(Theme.TEXT_MAIN);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = Theme.createTextField(20);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Password
        JLabel passLabel = new JLabel("Password (min 6 characters)");
        passLabel.setFont(Theme.FONT_BOLD);
        passLabel.setForeground(Theme.TEXT_MAIN);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = Theme.createPasswordField(20);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Confirm Password
        JLabel confirmPassLabel = new JLabel("Confirm Password");
        confirmPassLabel.setFont(Theme.FONT_BOLD);
        confirmPassLabel.setForeground(Theme.TEXT_MAIN);
        confirmPassLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        confirmPasswordField = Theme.createPasswordField(20);
        confirmPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        confirmPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Role selection (Public registration allows Citizen or Volunteer)
        JLabel roleLabel = new JLabel("Select Account Type");
        roleLabel.setFont(Theme.FONT_BOLD);
        roleLabel.setForeground(Theme.TEXT_MAIN);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        roleComboBox = new JComboBox<>(new Role[]{Role.CITIZEN, Role.VOLUNTEER});
        roleComboBox.setFont(Theme.FONT_REGULAR);
        roleComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        roleComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleComboBox.setBackground(Color.WHITE);

        // Error / feedback label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(Theme.FONT_SMALL);
        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Submit Button
        JButton registerButton = Theme.createSuccessButton("Complete Registration");
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.addActionListener(e -> performRegister());

        // Back to Login Button
        JButton backToLoginBtn = new JButton("Already have an account? Sign In");
        backToLoginBtn.setFont(Theme.FONT_SMALL);
        backToLoginBtn.setForeground(Theme.ACCENT);
        backToLoginBtn.setBorderPainted(false);
        backToLoginBtn.setContentAreaFilled(false);
        backToLoginBtn.setFocusPainted(false);
        backToLoginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backToLoginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backToLoginBtn.addActionListener(e -> parentFrame.showLoginPanel());

        // Assembly
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(descLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(userLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(8));
        card.add(passLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(8));
        card.add(confirmPassLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(confirmPasswordField);
        card.add(Box.createVerticalStrut(8));
        card.add(roleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(roleComboBox);
        card.add(Box.createVerticalStrut(8));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(registerButton);
        card.add(Box.createVerticalStrut(6));
        card.add(backToLoginBtn);

        add(card);
    }

    private void performRegister() {
        String username = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());
        Role selectedRole = (Role) roleComboBox.getSelectedItem();

        if (username.length() < 3) {
            errorLabel.setText("Username must be at least 3 characters.");
            return;
        }
        if (pass.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters.");
            return;
        }
        if (!pass.equals(confirmPass)) {
            errorLabel.setText("Passwords do not match!");
            return;
        }

        try {
            User user = UserAuthenticationService.getInstance().registerPublic(username, pass, selectedRole);
            JOptionPane.showMessageDialog(this,
                    "Account successfully created for @" + user.getUsername() + "! Please sign in.",
                    "Registration Successful",
                    JOptionPane.INFORMATION_MESSAGE);
            parentFrame.showLoginPanel();
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
        }
    }
}
