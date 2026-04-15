package com.valinor.iposca.gui;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.ApplicationUser;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {

    private ApplicationUser currentUser;
    private JPanel contentArea;
    private JPanel sidebarPanel;
    private List<JPanel> navItems = new ArrayList<>();
    private int activeIndex = 0;

    // panel instances
    private JPanel[] panels;
    private String[] labels;
    private String[] subLabels;

    public MainFrame(ApplicationUser user) {
        this.currentUser = user;

        setTitle("IPOS-CA - InfoPharma Client Application (Team 14 - Valinor)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(950, 550));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseManager.closeConnection();
                System.exit(0);
            }
        });

        AppTheme.applyGlobalDefaults();
        initPanels();
        buildUI();
    }

    private void initPanels() {
        String role = currentUser.getRole();

        List<JPanel> panelList = new ArrayList<>();
        List<String> labelList = new ArrayList<>();
        List<String> subList = new ArrayList<>();

        if ("Admin".equals(role)) {
            panelList.add(new UserPanel());
            labelList.add("User Management");
            subList.add(null);
        }

        // All non-admin roles + admin get operational tabs
        panelList.add(new StockPanel());
        labelList.add("Local Stock");
        subList.add(null);

        panelList.add(new CustomerPanel(role));
        labelList.add("Customers");
        subList.add(null);

        panelList.add(new SalesPanel());
        labelList.add("Sales");
        subList.add(null);

        panelList.add(new SACataloguePanel());
        labelList.add("SA Catalogue");
        subList.add(null);

        panelList.add(new SAOrdersPanel());
        labelList.add("SA Orders");
        subList.add(null);

        panelList.add(new PUOrderPanel());
        labelList.add("PU Orders");
        subList.add("PU Order Management");

        panelList.add(new EmailPanel());
        labelList.add("Order Emails");
        subList.add(null);

        panelList.add(new TemplatePanel());
        labelList.add("Templates");
        subList.add(null);

        // Admin and Manager get Reports
        if ("Admin".equals(role) || "Manager".equals(role) || "Test".equals(role)) {
            panelList.add(new ReportPanel());
            labelList.add("Reports");
            subList.add(null);
        }

        // Test role also gets User Management if not Admin
        if ("Test".equals(role)) {
            panelList.add(new UserPanel());
            labelList.add("User Management");
            subList.add(null);
        }

        panels = panelList.toArray(new JPanel[0]);
        labels = labelList.toArray(new String[0]);
        subLabels = subList.toArray(new String[0]);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.bg());

        root.add(buildNavBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(AppTheme.bg());

        body.add(buildSidebar(), BorderLayout.WEST);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(AppTheme.bg());
        if (panels.length > 0) {
            contentArea.add(panels[0], BorderLayout.CENTER);
        }
        body.add(contentArea, BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildNavBar() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(AppTheme.navBar());
        nav.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        left.setOpaque(false);

        JLabel appName = new JLabel("IPOS-CA");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        appName.setForeground(Color.WHITE);
        left.add(appName);

        JLabel dot = new JLabel("\u2022");
        dot.setForeground(new Color(255, 255, 255, 80));
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        left.add(dot);

        JLabel userInfo = new JLabel(currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        userInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userInfo.setForeground(new Color(200, 225, 205));
        left.add(userInfo);

        nav.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        right.setOpaque(false);

        JButton toggleBtn = AppTheme.darkModeToggle();
        toggleBtn.addActionListener(e -> {
            AppTheme.toggle();
            AppTheme.applyGlobalDefaults();
            int saved = activeIndex;
            getContentPane().removeAll();
            initPanels();
            buildUI();
            switchTo(saved);
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

    private JPanel buildSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(AppTheme.sidebar());
        sidebarPanel.setPreferredSize(new Dimension(200, 0));
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.border()));

        navItems.clear();

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            JPanel item = new JPanel(new BorderLayout());
            item.setBackground(i == 0 ? AppTheme.sidebarActive() : AppTheme.sidebar());
            item.setMaximumSize(new Dimension(200, subLabels[i] != null ? 52 : 40));
            item.setPreferredSize(new Dimension(200, subLabels[i] != null ? 52 : 40));
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.setBorder(BorderFactory.createEmptyBorder(subLabels[i] != null ? 6 : 10, 18, 6, 10));

            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(new Font("Segoe UI", i == 0 ? Font.BOLD : Font.PLAIN, 13));
            lbl.setForeground(i == 0 ? Color.WHITE : AppTheme.sidebarText());
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(lbl);

            if (subLabels[i] != null) {
                JLabel sub = new JLabel(subLabels[i]);
                sub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                sub.setForeground(AppTheme.sidebarText().darker());
                sub.setAlignmentX(Component.LEFT_ALIGNMENT);
                textPanel.add(sub);
            }

            item.add(textPanel, BorderLayout.CENTER);

            // Active indicator bar on left
            JPanel indicator = new JPanel();
            indicator.setPreferredSize(new Dimension(3, 0));
            indicator.setBackground(i == 0 ? AppTheme.accent() : AppTheme.sidebar());
            item.add(indicator, BorderLayout.WEST);

            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) { switchTo(idx); }
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (idx != activeIndex) item.setBackground(AppTheme.sidebarHover());
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (idx != activeIndex) item.setBackground(AppTheme.sidebar());
                }
            });

            navItems.add(item);
            sidebarPanel.add(item);
        }

        // Push everything to the top
        sidebarPanel.add(Box.createVerticalGlue());

        return sidebarPanel;
    }

    private void switchTo(int index) {
        if (index < 0 || index >= panels.length) return;
        activeIndex = index;

        // Update sidebar highlighting
        for (int i = 0; i < navItems.size(); i++) {
            JPanel item = navItems.get(i);
            boolean active = (i == index);
            item.setBackground(active ? AppTheme.sidebarActive() : AppTheme.sidebar());

            // Update indicator
            Component indicator = item.getComponent(1);
            if (indicator instanceof JPanel) {
                ((JPanel) indicator).setBackground(active ? AppTheme.accent() : AppTheme.sidebar());
            }

            // Update label style
            JPanel textPanel = (JPanel) item.getComponent(0);
            JLabel lbl = (JLabel) textPanel.getComponent(0);
            lbl.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
            lbl.setForeground(active ? Color.WHITE : AppTheme.sidebarText());
        }

        // Switch content
        contentArea.removeAll();
        contentArea.add(panels[index], BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
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
