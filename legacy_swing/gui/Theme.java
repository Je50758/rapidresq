package gui;

import auth.Role;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Modern Flat Theme utility defining unified colors, typography,
 * card containers, and UI widget helpers.
 */
public class Theme {
    // Brand & Base Colors
    public static final Color PRIMARY = new Color(30, 41, 59);          // Slate 800
    public static final Color PRIMARY_HOVER = new Color(51, 65, 85);    // Slate 700
    public static final Color ACCENT = new Color(79, 70, 229);          // Indigo 600
    public static final Color ACCENT_HOVER = new Color(67, 56, 202);    // Indigo 700
    public static final Color BACKGROUND = new Color(248, 250, 252);     // Light Gray 50
    public static final Color CARD_BG = Color.WHITE;
    public static final Color BORDER = new Color(226, 232, 240);        // Slate 200
    public static final Color TEXT_MAIN = new Color(15, 23, 42);        // Slate 900
    public static final Color TEXT_MUTED = new Color(100, 116, 139);    // Slate 500
    public static final Color SUCCESS = new Color(16, 185, 129);        // Emerald 500
    public static final Color DANGER = new Color(239, 68, 68);          // Red 500
    public static final Color WARNING = new Color(245, 158, 11);        // Amber 500
    public static final Color INFO = new Color(59, 130, 246);           // Blue 500

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    /**
     * Creates a styled card container panel.
     */
    public static JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
        return card;
    }

    /**
     * Creates a modern flat button.
     */
    public static JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    public static JButton createPrimaryButton(String text) {
        return createButton(text, ACCENT, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = createButton(text, BACKGROUND, TEXT_MAIN);
        btn.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(8, 16, 8, 16)));
        return btn;
    }

    public static JButton createDangerButton(String text) {
        return createButton(text, DANGER, Color.WHITE);
    }

    public static JButton createSuccessButton(String text) {
        return createButton(text, SUCCESS, Color.WHITE);
    }

    /**
     * Creates a styled text field with rounded-like light padding.
     */
    public static JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(FONT_REGULAR);
        field.setForeground(TEXT_MAIN);
        field.setCaretColor(TEXT_MAIN);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }

    public static JPasswordField createPasswordField(int columns) {
        JPasswordField field = new JPasswordField(columns);
        field.setFont(FONT_REGULAR);
        field.setForeground(TEXT_MAIN);
        field.setCaretColor(TEXT_MAIN);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }

    /**
     * Renders a role badge label with role-specific color styling.
     */
    public static JLabel createRoleBadge(Role role) {
        JLabel badge = new JLabel("  " + role.getDisplayName() + "  ");
        badge.setOpaque(true);
        badge.setBackground(role.getBadgeBackground());
        badge.setForeground(role.getBadgeColor());
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setBorder(new LineBorder(role.getBadgeColor(), 1, true));
        return badge;
    }
}
