package com.valinor.iposca.gui;

import com.valinor.iposca.dao.StockDAO;
import com.valinor.iposca.model.StockItem;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Stock management screen. View, add, edit, delete stock items.
 * Record deliveries, set markup/VAT, view low stock report.
 */
public class StockPanel extends JPanel {

    private StockDAO stockDAO;
    private JTable stockTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    private final String[] columns = {
            "Item ID", "Description", "Package Type", "Unit", "Units in Pack",
            "Package Cost (£)", "Markup %", "Retail (£)", "Availability", "Stock Limit", "Status"
    };

    public StockPanel() {
        stockDAO = new StockDAO();
        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.bg());

        add(AppTheme.headerBar("Stock Management"), BorderLayout.NORTH);

        // content area: search + table
        JPanel content = AppTheme.contentPanel();

        // search bar
        Object[] sb = AppTheme.searchBar(this::performSearch, this::refreshTable);
        JPanel searchPanel = (JPanel) sb[0];
        searchField = (JTextField) sb[1];

        // extra buttons on the search row
        JButton vatBtn = AppTheme.outlineBtn("Configure VAT");
        vatBtn.addActionListener(e -> configureVATRate());
        searchPanel.add(vatBtn);

        JButton lowBtn = AppTheme.outlineBtn("Low Stock Report");
        lowBtn.addActionListener(e -> showLowStockReport());
        searchPanel.add(lowBtn);

        content.add(searchPanel, BorderLayout.NORTH);

        // table
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        stockTable = new JTable(tableModel);
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        AppTheme.styleTable(stockTable);

        // colour the status column
        stockTable.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                                                           boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    if ("LOW STOCK".equals(val)) {
                        setBackground(AppTheme.lowStockBg());
                        setForeground(AppTheme.red());
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setBackground(row % 2 == 0 ? AppTheme.surface() : AppTheme.tableAlt());
                        setForeground(AppTheme.green());
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

        JScrollPane sp = new JScrollPane(stockTable);
        AppTheme.styleScrollPane(sp);
        content.add(sp, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        // button bar at bottom
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        bar.setBackground(AppTheme.bg());
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JButton addBtn = AppTheme.btn("Add Item");
        addBtn.addActionListener(e -> showAddItemDialog());
        bar.add(addBtn);

        JButton editBtn = AppTheme.btn("Edit Item");
        editBtn.addActionListener(e -> showEditItemDialog());
        bar.add(editBtn);

        JButton delBtn = AppTheme.btn("Delete Item");
        delBtn.addActionListener(e -> deleteSelectedItem());
        bar.add(delBtn);

        JButton delivBtn = AppTheme.btn("Record Delivery");
        delivBtn.addActionListener(e -> showRecordDeliveryDialog());
        bar.add(delivBtn);

        JButton mkBtn = AppTheme.btn("Set Markup");
        mkBtn.addActionListener(e -> showSetMarkupDialog());
        bar.add(mkBtn);

        JButton refBtn = AppTheme.btn("Refresh");
        refBtn.addActionListener(e -> refreshTable());
        bar.add(refBtn);

        add(bar, BorderLayout.SOUTH);

        refreshTable();
        showLowStockWarningIfNeeded();
    }

    // ==================== DATA ====================

    private void refreshTable() {
        tableModel.setRowCount(0);
        double vat = stockDAO.getVATRate();
        for (StockItem item : stockDAO.getAllStockItems()) {
            String status = item.isLowStock() ? "LOW STOCK" : "OK";
            tableModel.addRow(new Object[]{
                    item.getItemId(), item.getDescription(), item.getPackageType(),
                    item.getUnit(), item.getUnitsInPack(),
                    String.format("%.2f", item.getBulkCost()),
                    String.format("%.1f%%", item.getMarkupRate()),
                    String.format("%.2f", item.getRetailPriceWithVAT(vat)),
                    item.getAvailability(), item.getStockLimit(), status
            });
        }
    }

    private void performSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { refreshTable(); return; }

        tableModel.setRowCount(0);
        double vat = stockDAO.getVATRate();
        List<StockItem> results = stockDAO.searchStockItems(kw);
        for (StockItem item : results) {
            String status = item.isLowStock() ? "LOW STOCK" : "OK";
            tableModel.addRow(new Object[]{
                    item.getItemId(), item.getDescription(), item.getPackageType(),
                    item.getUnit(), item.getUnitsInPack(),
                    String.format("%.2f", item.getBulkCost()),
                    String.format("%.1f%%", item.getMarkupRate()),
                    String.format("%.2f", item.getRetailPriceWithVAT(vat)),
                    item.getAvailability(), item.getStockLimit(), status
            });
        }
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items found matching: " + kw,
                    "Search", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ==================== DIALOGS ====================

    private void showAddItemDialog() {
        JTextField idF = new JTextField(15), descF = new JTextField(15),
                pkgF = new JTextField(10), unitF = new JTextField(10),
                uipF = new JTextField(5), costF = new JTextField(10),
                mkF = new JTextField(5), avF = new JTextField(5), limF = new JTextField(5);

        JPanel f = new JPanel(new GridLayout(9, 2, 5, 5));
        f.add(new JLabel("Item ID (e.g. 100 00001):")); f.add(idF);
        f.add(new JLabel("Description:"));               f.add(descF);
        f.add(new JLabel("Package Type (e.g. box):"));   f.add(pkgF);
        f.add(new JLabel("Unit (e.g. Caps, ml):"));      f.add(unitF);
        f.add(new JLabel("Units in Pack:"));              f.add(uipF);
        f.add(new JLabel("Package Cost (£):"));         f.add(costF);
        f.add(new JLabel("Markup Rate (%):"));            f.add(mkF);
        f.add(new JLabel("Initial Availability:"));       f.add(avF);
        f.add(new JLabel("Low Stock Limit:"));            f.add(limF);

        if (JOptionPane.showConfirmDialog(this, f, "Add New Stock Item",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                StockItem item = new StockItem(
                        idF.getText().trim(), descF.getText().trim(), pkgF.getText().trim(),
                        unitF.getText().trim(), Integer.parseInt(uipF.getText().trim()),
                        Double.parseDouble(costF.getText().trim()),
                        Double.parseDouble(mkF.getText().trim()),
                        Integer.parseInt(avF.getText().trim()),
                        Integer.parseInt(limF.getText().trim())
                );
                if (item.getItemId().isEmpty() || item.getDescription().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Item ID and Description are required.");
                    return;
                }
                if (stockDAO.addStockItem(item)) {
                    JOptionPane.showMessageDialog(this, "Item added.");
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed — ID may already exist.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Enter valid numbers for numeric fields.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditItemDialog() {
        int r = stockTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select an item to edit."); return; }

        String id = (String) tableModel.getValueAt(r, 0);
        StockItem item = stockDAO.getStockItemById(id);
        if (item == null) { JOptionPane.showMessageDialog(this, "Item not found."); return; }

        JTextField descF = new JTextField(item.getDescription(), 15),
                pkgF = new JTextField(item.getPackageType(), 10),
                unitF = new JTextField(item.getUnit(), 10),
                uipF = new JTextField(""+item.getUnitsInPack(), 5),
                costF = new JTextField(""+item.getBulkCost(), 10),
                mkF = new JTextField(""+item.getMarkupRate(), 5),
                avF = new JTextField(""+item.getAvailability(), 5),
                limF = new JTextField(""+item.getStockLimit(), 5);

        JPanel f = new JPanel(new GridLayout(8, 2, 5, 5));
        f.add(new JLabel("Description:"));    f.add(descF);
        f.add(new JLabel("Package Type:"));   f.add(pkgF);
        f.add(new JLabel("Unit:"));           f.add(unitF);
        f.add(new JLabel("Units in Pack:"));  f.add(uipF);
        f.add(new JLabel("Package Cost (£):")); f.add(costF);
        f.add(new JLabel("Markup (%):"));     f.add(mkF);
        f.add(new JLabel("Availability:"));   f.add(avF);
        f.add(new JLabel("Stock Limit:"));    f.add(limF);

        if (JOptionPane.showConfirmDialog(this, f, "Edit: " + id,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                item.setDescription(descF.getText().trim());
                item.setPackageType(pkgF.getText().trim());
                item.setUnit(unitF.getText().trim());
                item.setUnitsInPack(Integer.parseInt(uipF.getText().trim()));
                item.setBulkCost(Double.parseDouble(costF.getText().trim()));
                item.setMarkupRate(Double.parseDouble(mkF.getText().trim()));
                item.setAvailability(Integer.parseInt(avF.getText().trim()));
                item.setStockLimit(Integer.parseInt(limF.getText().trim()));
                if (stockDAO.updateStockItem(item)) {
                    JOptionPane.showMessageDialog(this, "Item updated.");
                    refreshTable();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelectedItem() {
        int r = stockTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select an item to delete."); return; }

        String id = (String) tableModel.getValueAt(r, 0);
        String desc = (String) tableModel.getValueAt(r, 1);
        int avail = Integer.parseInt(tableModel.getValueAt(r, 8).toString());

        String msg = avail > 0
                ? "WARNING: \"" + desc + "\" still has " + avail + " packs in stock.\nDelete anyway?"
                : "Delete " + id + " - " + desc + "?";

        if (JOptionPane.showConfirmDialog(this, msg,
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            if (stockDAO.deleteStockItem(id)) {
                JOptionPane.showMessageDialog(this, "Deleted.");
                refreshTable();
            }
        }
    }


    private void showRecordDeliveryDialog() {
        int r = stockTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select an item first."); return; }

        String id = (String) tableModel.getValueAt(r, 0);
        String desc = (String) tableModel.getValueAt(r, 1);

        JTextField qtyF = new JTextField(10), notesF = new JTextField(20);
        JPanel f = new JPanel(new GridLayout(3, 2, 5, 5));
        f.add(new JLabel("Item:")); f.add(new JLabel(id + " - " + desc));
        f.add(new JLabel("Quantity (packs):")); f.add(qtyF);
        f.add(new JLabel("Notes (optional):")); f.add(notesF);

        if (JOptionPane.showConfirmDialog(this, f, "Record Delivery",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                int qty = Integer.parseInt(qtyF.getText().trim());
                if (qty <= 0) { JOptionPane.showMessageDialog(this, "Quantity must be > 0."); return; }
                String notes = notesF.getText().trim();
                if (stockDAO.recordDelivery(id, qty, notes.isEmpty() ? null : notes)) {
                    JOptionPane.showMessageDialog(this, "Delivery recorded. +" + qty + " packs.");
                    refreshTable();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showSetMarkupDialog() {
        int r = stockTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select an item."); return; }
        String id = (String) tableModel.getValueAt(r, 0);
        StockItem item = stockDAO.getStockItemById(id);
        if (item == null) return;

        String input = JOptionPane.showInputDialog(this,
                "Markup rate (%) for " + item.getDescription() + ":", "" + item.getMarkupRate());
        if (input != null) {
            try {
                double rate = Double.parseDouble(input.trim());
                if (rate < 0) { JOptionPane.showMessageDialog(this, "Can't be negative."); return; }
                item.setMarkupRate(rate);
                if (stockDAO.updateStockItem(item)) {
                    JOptionPane.showMessageDialog(this, "Markup set to " + rate + "%.");
                    refreshTable();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void configureVATRate() {
        double cur = stockDAO.getVATRate();
        String input = JOptionPane.showInputDialog(this, "VAT rate (%).\nCurrent: " + cur + "%", "" + cur);
        if (input != null) {
            try {
                double rate = Double.parseDouble(input.trim());
                if (rate < 0) { JOptionPane.showMessageDialog(this, "Can't be negative."); return; }
                if (stockDAO.setVATRate(rate)) {
                    JOptionPane.showMessageDialog(this, "VAT set to " + rate + "%.");
                    refreshTable();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showLowStockReport() {
        List<StockItem> low = stockDAO.getLowStockItems();
        if (low.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All items above stock limits.", "Low Stock", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s %-20s %-10s %-10s %-12s\n", "Item ID", "Description", "Available", "Limit", "Order Qty"));
        sb.append("-".repeat(68)).append("\n");
        for (StockItem i : low) {
            int rec = (int) Math.ceil(i.getStockLimit() * 1.10) - i.getAvailability();
            if (rec < 0) rec = 0;
            sb.append(String.format("%-15s %-20s %-10d %-10d %-12d\n",
                    i.getItemId(), i.getDescription(), i.getAvailability(), i.getStockLimit(), rec));
        }
        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(AppTheme.MONO);
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(550, 280));
        JOptionPane.showMessageDialog(this, sp, "Low Stock — " + low.size() + " item(s)", JOptionPane.WARNING_MESSAGE);
    }

    private void showLowStockWarningIfNeeded() {
        List<StockItem> low = stockDAO.getLowStockItems();
        if (!low.isEmpty()) {
            StringBuilder msg = new StringBuilder(low.size() + " item(s) below stock limit:\n\n");
            for (StockItem i : low) {
                msg.append("  • ").append(i.getDescription())
                        .append(" (").append(i.getAvailability()).append("/").append(i.getStockLimit()).append(")\n");
            }
            msg.append("\nConsider ordering from InfoPharma.");
            JOptionPane.showMessageDialog(this, msg.toString(), "Low Stock Warning", JOptionPane.WARNING_MESSAGE);
        }
    }
}