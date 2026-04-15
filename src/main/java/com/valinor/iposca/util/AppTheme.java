package com.valinor.iposca.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class AppTheme {

    private static boolean darkMode = false;

    // green

    private static final Color L_BG = new Color(243, 248, 243);
    private static final Color L_SURFACE = Color.WHITE;
    private static final Color L_SURFACE_ALT = new Color(237, 245, 238);
    private static final Color L_TEXT = new Color(28, 38, 30);
    private static final Color L_TEXT_SEC = new Color(100, 120, 105);
    private static final Color L_BORDER = new Color(200, 218, 200);
    private static final Color L_ACCENT = new Color(46, 139, 87);
    private static final Color L_TABLE_HEADER = new Color(238, 246, 240);
    private static final Color L_TABLE_ALT = new Color(246, 250, 247);
    private static final Color L_SELECTION = new Color(200, 235, 210);

    // dark green

    private static final Color D_BG = new Color(22, 28, 24);
    private static final Color D_SURFACE = new Color(30, 38, 33);
    private static final Color D_SURFACE_ALT = new Color(38, 48, 42);
    private static final Color D_TEXT = new Color(220, 232, 222);
    private static final Color D_TEXT_SEC = new Color(130, 155, 135);
    private static final Color D_BORDER = new Color(50, 65, 55);
    private static final Color D_ACCENT = new Color(72, 180, 110);
    private static final Color D_TABLE_HEADER = new Color(26, 33, 28);
    private static final Color D_TABLE_ALT = new Color(35, 44, 38);
    private static final Color D_SELECTION = new Color(40, 80, 55);

    // shared colours

    private static final Color GREEN = new Color(46, 139, 87);
    private static final Color GREEN_DARK = new Color(38, 120, 72);
    private static final Color RED = new Color(210, 60, 70);
    private static final Color RED_DARK = new Color(190, 50, 55);
    private static final Color ORANGE = new Color(230, 150, 30);

    // sidebar colours

    public static Color sidebar() { return darkMode ? new Color(24, 32, 26) : new Color(36, 68, 45); }
    public static Color sidebarText() { return darkMode ? new Color(170, 195, 175) : new Color(200, 225, 205); }
    public static Color sidebarActive() { return darkMode ? new Color(40, 60, 45) : new Color(50, 90, 60); }
    public static Color sidebarHover() { return darkMode ? new Color(34, 46, 38) : new Color(44, 80, 55); }


    public static Color bg() { return darkMode ? D_BG : L_BG; }
    public static Color surface() { return darkMode ? D_SURFACE : L_SURFACE; }
    public static Color surfaceAlt() { return darkMode ? D_SURFACE_ALT : L_SURFACE_ALT; }
    public static Color text() { return darkMode ? D_TEXT : L_TEXT; }
    public static Color textSec() { return darkMode ? D_TEXT_SEC : L_TEXT_SEC; }
    public static Color border() { return darkMode ? D_BORDER : L_BORDER; }
    public static Color accent() { return darkMode ? D_ACCENT : L_ACCENT; }
    public static Color tableHeader() { return darkMode ? D_TABLE_HEADER : L_TABLE_HEADER; }
    public static Color tableAlt() { return darkMode ? D_TABLE_ALT : L_TABLE_ALT; }
    public static Color selection() { return darkMode ? D_SELECTION : L_SELECTION; }
    public static Color green() { return GREEN; }
    public static Color red() { return RED; }
    public static Color orange() { return ORANGE; }
    public static Color lowStockBg() { return darkMode ? new Color(55, 30, 28) : new Color(255, 240, 240); }

    public static Color headerBar() { return darkMode ? D_SURFACE : new Color(36, 68, 45); }
    public static Color headerText() { return Color.WHITE; }

    public static Color navBar() { return darkMode ? new Color(24, 32, 26) : new Color(36, 68, 45); }

    // Fonts

    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font SUBTITLE = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font TABLE = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font TABLE_HEAD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font BTN = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font MONO = new Font("Consolas", Font.PLAIN, 12);

    // Dark mode

    public static boolean isDark() { return darkMode; }
    public static void setDark(boolean d) { darkMode = d; }
    public static void toggle() { darkMode = !darkMode; }

    //

    public static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;
        private final Insets insets;

        public RoundedBorder(int radius, Color color, Insets insets) {
            this.radius = radius;
            this.color = color;
            this.insets = insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) { return insets; }
    }

    // Buttons

    public static JButton primaryBtn(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(BTN);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBackground(accent());
        b.setForeground(Color.WHITE);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(7, 18, 7, 18));
        return b;
    }

    public static JButton btn(String text) {
        return outlineBtn(text);
    }

    public static JButton greenBtn(String text) {
        return outlineBtn(text);
    }

    public static JButton redBtn(String text) {
        return outlineBtn(text);
    }

    public static JButton outlineBtn(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(BTN);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBackground(surface());
        b.setForeground(text());
        b.setBorder(new RoundedBorder(12, border(), new Insets(7, 18, 7, 18)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // Dark mode toggle

    public static JButton darkModeToggle() {
        String icon = darkMode ? "\u2600" : "\u263D";
        JButton b = new JButton(icon);
        b.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        b.setForeground(Color.WHITE);
        b.setBackground(navBar());
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setToolTipText(darkMode ? "Switch to light mode" : "Switch to dark mode");
        return b;
    }

    // Table Styling

    public static void styleTable(JTable table) {
        table.setFont(TABLE);
        table.setRowHeight(32);
        table.setGridColor(border());
        table.setSelectionBackground(selection());
        table.setSelectionForeground(text());
        table.setBackground(surface());
        table.setForeground(text());
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setReorderingAllowed(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(TABLE_HEAD);
        header.setBackground(tableHeader());
        header.setForeground(darkMode ? D_TEXT_SEC : L_TEXT);
        header.setPreferredSize(new Dimension(0, 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, border()));

        DefaultTableCellRenderer headRender = new DefaultTableCellRenderer();
        headRender.setBackground(tableHeader());
        headRender.setForeground(darkMode ? D_TEXT_SEC : L_TEXT);
        headRender.setFont(TABLE_HEAD);
        headRender.setHorizontalAlignment(SwingConstants.LEFT);
        headRender.setBorder(new EmptyBorder(0, 10, 0, 10));
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headRender);
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? AppTheme.surface() : AppTheme.tableAlt());
                    setForeground(AppTheme.text());
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
    }

    // Panel helpers

    public static JPanel contentPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(bg());
        p.setBorder(new EmptyBorder(12, 15, 12, 15));
        return p;
    }

    public static JPanel headerBar(String title) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(headerBar());
        h.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel l = new JLabel(title);
        l.setFont(TITLE);
        l.setForeground(headerText());
        h.add(l, BorderLayout.WEST);

        return h;
    }

    public static Object[] searchBar(Runnable onSearch, Runnable onClear) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        p.setOpaque(false);

        JLabel lbl = new JLabel("Search:");
        lbl.setFont(BODY);
        lbl.setForeground(text());
        p.add(lbl);

        JTextField f = new JTextField(20);
        f.setFont(BODY);
        f.setPreferredSize(new Dimension(220, 30));
        f.setBackground(surface());
        f.setForeground(text());
        f.setCaretColor(text());
        f.setBorder(new RoundedBorder(10, border(), new Insets(4, 8, 4, 8)));
        p.add(f);

        JButton searchBtn = primaryBtn("Search");
        searchBtn.addActionListener(e -> onSearch.run());
        p.add(searchBtn);

        JButton clearBtn = outlineBtn("Show All");
        clearBtn.addActionListener(e -> {
            f.setText("");
            onClear.run();
        });
        p.add(clearBtn);

        f.addActionListener(e -> onSearch.run());

        return new Object[]{p, f};
    }

    public static void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createLineBorder(border(), 1));
        sp.getViewport().setBackground(surface());
    }

    public static void applyGlobalDefaults() {
        UIManager.put("Panel.background", bg());
        UIManager.put("OptionPane.background", bg());
        UIManager.put("OptionPane.messageForeground", text());
        UIManager.put("Label.foreground", text());
        UIManager.put("TextField.background", surface());
        UIManager.put("TextField.foreground", text());
        UIManager.put("TextField.caretForeground", text());
        UIManager.put("ComboBox.background", surface());
        UIManager.put("ComboBox.foreground", text());
        UIManager.put("Button.background", surface());
        UIManager.put("Button.foreground", text());
        UIManager.put("TabbedPane.background", bg());
        UIManager.put("TabbedPane.foreground", text());
        UIManager.put("TabbedPane.selected", surface());
        UIManager.put("TabbedPane.contentAreaColor", bg());
        UIManager.put("TabbedPane.light", border());
        UIManager.put("TabbedPane.shadow", border());
        UIManager.put("TabbedPane.darkShadow", border());
        UIManager.put("TabbedPane.highlight", border());
        UIManager.put("ScrollPane.background", surface());
        UIManager.put("Table.background", surface());
        UIManager.put("Table.foreground", text());
        UIManager.put("TableHeader.background", tableHeader());
        UIManager.put("TableHeader.foreground", text());
    }
}
