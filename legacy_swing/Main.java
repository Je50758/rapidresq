import gui.AuthFrame;

import javax.swing.*;

/**
 * Main application launcher for the Rapid Disaster Response System.
 */
public class Main {
    public static void main(String[] args) {
        // Set native system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Launch initial Authentication window on Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            AuthFrame authFrame = new AuthFrame();
            authFrame.setVisible(true);
        });
    }
}
