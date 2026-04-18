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

//Full-screen sign in window with a centered login card.
public class SignInFrame extends JFrame {

    private UserDAO userDAO;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel messageLabel;

    private static final String USERNAME_PLACEHOLDER = "Username";
    private static final String PASSWORD_PLACEHOLDER = "Password";

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

        AppTheme.applyGlobalDefaults();
        buildUI();
    }

    private void buildUI() {
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(AppTheme.bg());

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.surface());
        card.setBorder(BorderFactory.createCompoundBorder(
                new AppTheme.RoundedBorder(16, AppTheme.border(), new Insets(1, 1, 1, 1)),
                new EmptyBorder(35, 40, 35, 40)
        ));
        card.setPreferredSize(new Dimension(380, 370));
        card.setMaximumSize(new Dimension(380, 370));

        JLabel title = new JLabel("IPOS-CA");
        title.setFont(AppTheme.TITLE.deriveFont(22f));
        title.setForeground(AppTheme.text());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);

        card.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Valinor Ltd - Pharmacy Management System");
        subtitle.setFont(AppTheme.BODY.deriveFont(11f));
        subtitle.setForeground(AppTheme.textSec());
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);

        card.add(Box.createVerticalStrut(28));

        usernameField = createStyledField(USERNAME_PLACEHOLDER);
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(300, 36));
        card.add(usernameField);

        card.add(Box.createVerticalStrut(12));

        passwordField = createStyledPasswordField(PASSWORD_PLACEHOLDER);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(300, 36));
        card.add(passwordField);

        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptLogin();
                }
            }
        });

        card.add(Box.createVerticalStrut(8));

        messageLabel = new JLabel(" ");
        messageLabel.setFont(AppTheme.BODY.deriveFont(11f));
        messageLabel.setForeground(AppTheme.red());
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(messageLabel);

        card.add(Box.createVerticalStrut(8));

        JButton signInBtn = AppTheme.primaryBtn("SIGN IN");
        signInBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signInBtn.setMaximumSize(new Dimension(300, 38));
        signInBtn.addActionListener(e -> attemptLogin());
        card.add(signInBtn);

        card.add(Box.createVerticalStrut(16));

        JButton toggleBtn = AppTheme.darkModeToggle();
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

        JButton testBtn = AppTheme.outlineBtn("SKIP LOGIN (TESTING)");
        testBtn.setFont(AppTheme.BODY.deriveFont(11f));
        testBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        testBtn.setMaximumSize(new Dimension(180, 32));
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


    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (USERNAME_PLACEHOLDER.equals(username)) {
            username = "";
        }
        if (PASSWORD_PLACEHOLDER.equals(password)) {
            password = "";
        }

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password.");
            return;
        }

        ApplicationUser user = userDAO.getUserFromUsername(username);
        if (user == null) {
            messageLabel.setText("Invalid username.");
            passwordField.setText("");
            passwordField.setEchoChar((char) 0);
            passwordField.setText(PASSWORD_PLACEHOLDER);
            passwordField.setForeground(AppTheme.textSec());
            return;
        }

        if (!user.getPassword().equals(password)) {
            messageLabel.setText("Incorrect password.");
            passwordField.setText("");
            passwordField.setEchoChar((char) 0);
            passwordField.setText(PASSWORD_PLACEHOLDER);
            passwordField.setForeground(AppTheme.textSec());
            return;
        }

        MainFrame frame = new MainFrame(user);
        frame.setVisible(true);
        this.dispose();
    }


    private JTextField createStyledField(String placeholder) {
        JTextField field = new JTextField(placeholder);
        field.setFont(AppTheme.BODY);
        field.setForeground(AppTheme.textSec());
        field.setBackground(AppTheme.surface());
        field.setCaretColor(AppTheme.text());
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setBorder(new AppTheme.RoundedBorder(10, AppTheme.border(), new Insets(6, 10, 6, 10)));

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
                if (field.getText().trim().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(AppTheme.textSec());
                }
            }
        });

        return field;
    }

    // password
    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setFont(AppTheme.BODY);
        field.setForeground(AppTheme.textSec());
        field.setBackground(AppTheme.surface());
        field.setCaretColor(AppTheme.text());
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setBorder(new AppTheme.RoundedBorder(10, AppTheme.border(), new Insets(6, 10, 6, 10)));

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