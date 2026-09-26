package gui;

import auth.User;

import javax.swing.*;
import java.awt.*;

/**
 * Authentication window displaying Login and Registration cards.
 * Serves as the initial entry screen of the application.
 */
public class AuthFrame extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel containerPanel;
    private final LoginPanel loginPanel;
    private final RegisterPanel registerPanel;

    public static final String CARD_LOGIN = "LOGIN";
    public static final String CARD_REGISTER = "REGISTER";

    public AuthFrame() {
        super("RapidResQ - Disaster Response Authentication");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 680);
        setMinimumSize(new Dimension(460, 620));
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        containerPanel = new JPanel(cardLayout);

        loginPanel = new LoginPanel(this);
        registerPanel = new RegisterPanel(this);

        containerPanel.add(loginPanel, CARD_LOGIN);
        containerPanel.add(registerPanel, CARD_REGISTER);

        setContentPane(containerPanel);
    }

    public void showLoginPanel() {
        cardLayout.show(containerPanel, CARD_LOGIN);
    }

    public void showRegisterPanel() {
        cardLayout.show(containerPanel, CARD_REGISTER);
    }

    /**
     * Transition from Auth to Main Application once authenticated.
     */
    public void onLoginSuccessful(User user) {
        dispose(); // Close Auth window
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(user);
            mainFrame.setVisible(true);
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new AuthFrame().setVisible(true));
    }
}
