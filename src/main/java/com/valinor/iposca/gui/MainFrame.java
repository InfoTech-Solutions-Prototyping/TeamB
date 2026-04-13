package com.valinor.iposca.gui;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.ApplicationUser;
import com.valinor.iposca.util.AppTheme;
import com.valinor.iposca.gui.SACataloguePanel;


import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * Main window. Has a nav bar at the top with user info,
 * dark/light toggle, and sign out. Content area uses tabs.
 */
public class MainFrame extends JFrame {

    private ApplicationUser currentUser;

    public MainFrame(ApplicationUser user) {
        this.currentUser = user;

        setTitle("IPOS-CA - InfoPharma Client Application (Team 14 - Valinor)");
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
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.bg());

        root.add(buildNavBar(), BorderLayout.NORTH);
        root.add(buildTabs(), BorderLayout.CENTER);

        setContentPane(root);
    }

    /**
     * Dark nav bar across the top with app name, user, toggle, sign out.
     */
    private JPanel buildNavBar() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(AppTheme.navBar());
        nav.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        // left: app name + user
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        left.setOpaque(false);

        JLabel appName = new JLabel("IPOS-CA");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        appName.setForeground(Color.WHITE);
        left.add(appName);

        JLabel dot = new JLabel("\u2022");
        dot.setForeground(new Color(255, 255, 255, 80));
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        left.add(dot);

        JLabel userInfo = new JLabel(currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        userInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userInfo.setForeground(new Color(180, 190, 210));
        left.add(userInfo);

        nav.add(left, BorderLayout.WEST);

        // right: dark mode toggle + sign out
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        right.setOpaque(false);

        JButton toggleBtn = AppTheme.darkModeToggle();
        toggleBtn.addActionListener(e -> {
            AppTheme.toggle();
            AppTheme.applyGlobalDefaults();
            getContentPane().removeAll();
            buildUI();
            revalidate();
            repaint();
        });
        right.add(toggleBtn);

        JButton signOut = new JButton("Sign Out");
        signOut.setFont(AppTheme.BTN);
        signOut.setForeground(Color.WHITE);
        signOut.setBackground(AppTheme.red());
        signOut.setFocusPainted(false);
        signOut.setBorderPainted(false);
        signOut.setOpaque(true);
        signOut.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signOut.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        signOut.addActionListener(e -> {
            SignInFrame frame = new SignInFrame();
            frame.setVisible(true);
            dispose();
        });
        right.add(signOut);

        nav.add(right, BorderLayout.EAST);

        return nav;
    }

    /**
     * Tab area with role-based filtering.
     */
    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(AppTheme.SUBTITLE);
        tabs.setBackground(AppTheme.bg());
        tabs.setForeground(AppTheme.text());

        String role = currentUser.getRole();

        if ("Admin".equals(role)) {
            tabs.addTab("User Management", new UserPanel());
        } else {
            tabs.addTab("Stock", new StockPanel());
            tabs.addTab("Customers", new CustomerPanel());
            tabs.addTab("Sales", new SalesPanel());
            tabs.addTab("SA Catalogue", new SACataloguePanel());
            tabs.addTab("Orders (SA)", new SAOrdersPanel());
            tabs.addTab("Templates", placeholder("Template management - coming soon"));

            if ("Manager".equals(role)) {
                tabs.addTab("Reports", placeholder("Report generation - coming soon"));
            }
        }

        // test role shows everything - remove before demo
        if ("Test".equals(role)) {
            tabs.addTab("Reports", placeholder("Report generation - coming soon"));
            tabs.addTab("User Management", new UserPanel());
        }

        return tabs;
    }

    private JPanel placeholder(String msg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(AppTheme.bg());
        JLabel l = new JLabel(msg, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.ITALIC, 15));
        l.setForeground(AppTheme.textSec());
        p.add(l, BorderLayout.CENTER);
        return p;
    }
}