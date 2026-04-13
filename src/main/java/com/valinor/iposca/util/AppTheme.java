package com.valinor.iposca.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Central theme for the app. Has a light and dark palette.
 * Dark mode uses a deep navy, light mode is clean white.
 * Every panel pulls colours from here so the look stays consistent.
 */
public class AppTheme {

    private static boolean darkMode = false;

    // ==================== LIGHT PALETTE ====================

    private static final Color L_BG = new Color(247, 248, 250);
    private static final Color L_SURFACE = Color.WHITE;
    private static final Color L_SURFACE_ALT = new Color(240, 242, 245);
    private static final Color L_TEXT = new Color(30, 34, 42);
    private static final Color L_TEXT_SEC = new Color(120, 128, 140);
    private static final Color L_BORDER = new Color(218, 222, 228);
    private static final Color L_ACCENT = new Color(55, 120, 220);
    private static final Color L_TABLE_HEADER = new Color(245, 246, 248);
    private static final Color L_TABLE_ALT = new Color(250, 251, 253);
    private static final Color L_SELECTION = new Color(220, 235, 255);

    // ==================== DARK PALETTE ====================
    // deep navy like the reference, not pure black

    private static final Color D_BG = new Color(18, 20, 28);
    private static final Color D_SURFACE = new Color(28, 32, 42);
    private static final Color D_SURFACE_ALT = new Color(36, 40, 52);
    private static final Color D_TEXT = new Color(225, 228, 235);
    private static final Color D_TEXT_SEC = new Color(130, 138, 155);
    private static final Color D_BORDER = new Color(50, 55, 68);
    private static final Color D_ACCENT = new Color(80, 150, 245);
    private static final Color D_TABLE_HEADER = new Color(24, 27, 36);
    private static final Color D_TABLE_ALT = new Color(33, 37, 48);
    private static final Color D_SELECTION = new Color(40, 70, 120);

    // ==================== SHARED ACTION COLOURS ====================

    private static final Color GREEN = new Color(40, 167, 69);
    private static final Color GREEN_DARK = new Color(35, 150, 60);
    private static final Color RED = new Color(220, 53, 69);
    private static final Color RED_DARK = new Color(200, 45, 55);
    private static final Color ORANGE = new Color(240, 150, 30);

    // ==================== COLOUR GETTERS ====================

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
    public static Color lowStockBg() { return darkMode ? new Color(60, 25, 28) : new Color(255, 240, 240); }

    // header bar - subtle in light mode, blends in dark mode
    public static Color headerBar() { return darkMode ? D_SURFACE : new Color(55, 70, 100); }
    public static Color headerText() { return Color.WHITE; }

    // top nav bar
    public static Color navBar() { return darkMode ? new Color(22, 25, 35) : new Color(38, 50, 75); }

    // ==================== FONTS ====================

    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font SUBTITLE = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font TABLE = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font TABLE_HEAD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font BTN = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font MONO = new Font("Consolas", Font.PLAIN, 12);

    // ==================== DARK MODE ====================

    public static boolean isDark() { return darkMode; }
    public static void setDark(boolean d) { darkMode = d; }
    public static void toggle() { darkMode = !darkMode; }

    // ==================== BUTTON BUILDERS ====================

    /**
     * Primary action button - subtle filled background.
     * Used for the main action on a screen (e.g. Search, Checkout).
     */
    public static JButton primaryBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(BTN);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBackground(accent());
        b.setForeground(Color.WHITE);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(6, 16, 6, 16));
        return b;
    }

    /**
     * Standard button used for most actions. Clean outline style.
     * Same look in both light and dark mode - blends with the theme.
     */
    public static JButton btn(String text) {
        return outlineBtn(text);
    }

    /**
     * Kept for backwards compatibility but now just calls btn().
     */
    public static JButton greenBtn(String text) {
        return outlineBtn(text);
    }

    /**
     * Kept for backwards compatibility but now just calls btn().
     */
    public static JButton redBtn(String text) {
        return outlineBtn(text);
    }

    /**
     * Outlined button - the standard look for all buttons.
     * White/surface background, thin border, dark text.
     */
    public static JButton outlineBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(BTN);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBackground(surface());
        b.setForeground(text());
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border(), 1),
                new EmptyBorder(6, 16, 6, 16)
        ));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ==================== DARK MODE TOGGLE BUTTON ====================

    /**
     * Sun/moon toggle button for the nav bar.
     * Uses unicode characters as simple icons.
     */
    public static JButton darkModeToggle() {
        // moon for "switch to dark", sun for "switch to light"
        String icon = darkMode ? "\u2600" : "\u263D";  // sun or moon
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

    // ==================== TABLE STYLING ====================

    /**
     * Applies the full theme to a table: row height, colours, header, alternating rows.
     */
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

        // style the header
        JTableHeader header = table.getTableHeader();
        header.setFont(TABLE_HEAD);
        header.setBackground(tableHeader());
        header.setForeground(darkMode ? D_TEXT_SEC : L_TEXT);
        header.setPreferredSize(new Dimension(0, 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, border()));

        // per-column header renderer so colours actually apply on all platforms
        DefaultTableCellRenderer headRender = new DefaultTableCellRenderer();
        headRender.setBackground(tableHeader());
        headRender.setForeground(darkMode ? D_TEXT_SEC : L_TEXT);
        headRender.setFont(TABLE_HEAD);
        headRender.setHorizontalAlignment(SwingConstants.LEFT);
        headRender.setBorder(new EmptyBorder(0, 10, 0, 10));
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headRender);
        }

        // alternating rows
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

    // ==================== PANEL / LAYOUT HELPERS ====================

    /**
     * Standard content panel with padding and background.
     */
    public static JPanel contentPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(bg());
        p.setBorder(new EmptyBorder(12, 15, 12, 15));
        return p;
    }

    /**
     * Header strip at the top of each tab panel.
     */
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

    /**
     * Search bar with field and buttons. Returns {JPanel, JTextField}.
     */
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
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border(), 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
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

    /**
     * Styles a scroll pane border and background.
     */
    public static void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createLineBorder(border(), 1));
        sp.getViewport().setBackground(surface());
    }

    /**
     * Sets the global Swing defaults so dialogs, option panes etc match the theme.
     * Call this at startup and after toggling dark mode.
     */
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