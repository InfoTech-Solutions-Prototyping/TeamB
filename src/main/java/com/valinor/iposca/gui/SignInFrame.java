package com.valinor.iposca.gui;

import com.valinor.iposca.dao.UserDAO;
import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.ApplicationUser;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Full-screen sign in window with a centered login card.
 * Matches the rest of the app's theme (dark/light mode).
 */
public class SignInFrame extends JFrame {

    private UserDAO userDAO;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel messageLabel;

    public SignInFrame() {
        userDAO = new UserDAO();

        setTitle("IPOS-CA");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1150, 720);
        setMinimumSize(new Dimension(900, 500));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseManager.closeConnection();
                System.exit(0);
            }
        });

        buildUI();
    }

    private void buildUI() {
        // full dark/light background
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(AppTheme.isDark() ? new Color(18, 20, 28) : new Color(30, 34, 42));

        // the white/dark card in the center
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.surface());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.border(), 1),
                new EmptyBorder(35, 40, 35, 40)
        ));
        card.setPreferredSize(new Dimension(380, 370));
        card.setMaximumSize(new Dimension(380, 370));

        // app title
        JLabel title = new JLabel("IPOS-CA");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(AppTheme.text());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);

        card.add(Box.createVerticalStrut(4));

        // subtitle
        JLabel subtitle = new JLabel("Valinor Ltd - Pharmacy Management System");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(AppTheme.textSec());
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);

        card.add(Box.createVerticalStrut(28));

        // username field with placeholder styling
        usernameField = createStyledField("Username");
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(300, 36));
        card.add(usernameField);

        card.add(Box.createVerticalStrut(12));

        // password field
        passwordField = createStyledPasswordField("Password");
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(300, 36));
        card.add(passwordField);

        // press enter to sign in
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) attemptLogin();
            }
        });

        card.add(Box.createVerticalStrut(8));

        // error message label (hidden until needed)
        messageLabel = new JLabel(" ");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        messageLabel.setForeground(AppTheme.red());
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(messageLabel);

        card.add(Box.createVerticalStrut(8));

        // sign in button - dark flat style like the reference
        JButton signInBtn = new JButton("SIGN IN");
        signInBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        signInBtn.setForeground(Color.WHITE);
        signInBtn.setBackground(new Color(45, 50, 60));
        signInBtn.setFocusPainted(false);
        signInBtn.setBorderPainted(false);
        signInBtn.setOpaque(true);
        signInBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signInBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signInBtn.setMaximumSize(new Dimension(300, 38));
        signInBtn.addActionListener(e -> attemptLogin());
        card.add(signInBtn);

        card.add(Box.createVerticalStrut(16));

        // dark mode toggle at the bottom of the card
        JButton toggleBtn = new JButton(AppTheme.isDark() ? "\u2600" : "\u263D");
        toggleBtn.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        toggleBtn.setForeground(AppTheme.textSec());
        toggleBtn.setBackground(AppTheme.surface());
        toggleBtn.setFocusPainted(false);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setOpaque(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggleBtn.addActionListener(e -> {
            AppTheme.toggle();
            AppTheme.applyGlobalDefaults();
            getContentPane().removeAll();
            buildUI();
            revalidate();
            repaint();
        });
        card.add(toggleBtn);

        card.add(Box.createVerticalStrut(8));

        // skip login for testing - remove before final demo
        JButton testBtn = new JButton("SKIP LOGIN (TESTING)");
        testBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        testBtn.setForeground(AppTheme.textSec());
        testBtn.setBackground(AppTheme.surface());
        testBtn.setFocusPainted(false);
        testBtn.setBorderPainted(false);
        testBtn.setOpaque(false);
        testBtn.setContentAreaFilled(false);
        testBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        testBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        testBtn.addActionListener(e -> {
            ApplicationUser user = new ApplicationUser();
            user.setRole("Test");
            user.setUsername("Test");
            MainFrame frame = new MainFrame(user);
            frame.setVisible(true);
            this.dispose();
        });
        card.add(testBtn);

        bg.add(card);
        setContentPane(bg);
    }

    /**
     * Checks credentials and opens the main app if valid.
     */
    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password.");
            return;
        }

        ApplicationUser user = userDAO.getUserFromUsername(username);
        if (user == null) {
            messageLabel.setText("Invalid username.");
            passwordField.setText("");
            return;
        }

        if (!user.getPassword().equals(password)) {
            messageLabel.setText("Incorrect password.");
            passwordField.setText("");
            return;
        }

        // success - open main app
        MainFrame frame = new MainFrame(user);
        frame.setVisible(true);
        this.dispose();
    }

    /**
     * Creates a text field with a thin border and placeholder-style look.
     */
    private JTextField createStyledField(String placeholder) {
        JTextField field = new JTextField(placeholder);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(AppTheme.textSec());
        field.setBackground(AppTheme.surface());
        field.setCaretColor(AppTheme.text());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.border(), 1),
                new EmptyBorder(6, 10, 6, 10)
        ));

        // clear placeholder text on focus
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(AppTheme.text());
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(AppTheme.textSec());
                }
            }
        });

        return field;
    }

    /**
     * Creates a password field with placeholder text that clears on focus.
     */
    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(AppTheme.textSec());
        field.setBackground(AppTheme.surface());
        field.setCaretColor(AppTheme.text());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.border(), 1),
                new EmptyBorder(6, 10, 6, 10)
        ));

        // show placeholder as regular text, switch to dots on focus
        field.setEchoChar((char) 0);
        field.setText(placeholder);

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (String.valueOf(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setEchoChar('\u2022');
                    field.setForeground(AppTheme.text());
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getPassword().length == 0) {
                    field.setEchoChar((char) 0);
                    field.setText(placeholder);
                    field.setForeground(AppTheme.textSec());
                }
            }
        });

        return field;
    }
}