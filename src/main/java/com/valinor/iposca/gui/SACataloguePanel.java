package com.valinor.iposca.gui;

import com.valinor.iposca.dao.SACatalogueDAO;
import com.valinor.iposca.dao.SAConnectionManager;
import com.valinor.iposca.model.SACatalogueItem;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Browse and sync the InfoPharma (SA) product catalogue.
 * No login required — catalogue is publicly available.
 */
public class SACataloguePanel extends JPanel {

    private final SACatalogueDAO catalogueDAO;
    private JTable catalogueTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    private final String[] columns = {
            "Item ID", "Description", "Pkg Type", "Unit", "Units/Pack",
            "Cost/Unit (\u00a3)", "Pack Cost (\u00a3)", "SA Availability"
    };

    public SACataloguePanel() {
        this.catalogueDAO = new SACatalogueDAO();

        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.bg());

        add(AppTheme.headerBar("InfoPharma Catalogue (SA)"), BorderLayout.NORTH);

        JPanel content = AppTheme.contentPanel();

        // search bar + sync button
        Object[] sb = AppTheme.searchBar(this::performSearch, this::refreshTable);
        JPanel searchPanel = (JPanel) sb[0];
        searchField = (JTextField) sb[1];

        JButton syncBtn = AppTheme.primaryBtn("Sync Catalogue from SA");
        syncBtn.addActionListener(e -> syncCatalogue());
        searchPanel.add(syncBtn);

        content.add(searchPanel, BorderLayout.NORTH);

        // table
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        catalogueTable = new JTable(tableModel);
        catalogueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        AppTheme.styleTable(catalogueTable);

        JScrollPane sp = new JScrollPane(catalogueTable);
        AppTheme.styleScrollPane(sp);
        content.add(sp, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        // bottom buttons
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        bar.setBackground(AppTheme.bg());
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JButton refreshBtn = AppTheme.btn("Refresh");
        refreshBtn.addActionListener(e -> refreshTable());
        bar.add(refreshBtn);

        add(bar, BorderLayout.SOUTH);

        refreshTable();
    }

    private void syncCatalogue() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            SAConnectionManager tempConn = new SAConnectionManager();
            List<SACatalogueItem> items = tempConn.fetchCatalogue();

            if (items.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No items returned from SA. Check that SA's database is running.",
                        "Sync", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (catalogueDAO.replaceAll(items)) {
                refreshTable();
                JOptionPane.showMessageDialog(this,
                        "Catalogue synced. " + items.size() + " items loaded.",
                        "Sync Complete", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to save catalogue locally.",
                        "Sync Error", JOptionPane.ERROR_MESSAGE);
            }
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (SACatalogueItem item : catalogueDAO.getAll()) {
            tableModel.addRow(new Object[]{
                    item.getItemId(),
                    item.getDescription(),
                    item.getPackageType(),
                    item.getUnit(),
                    item.getUnitsPerPack(),
                    String.format("%.2f", item.getCostPerUnit()),
                    String.format("%.2f", item.getPackCost()),
                    item.getAvailability()
            });
        }
    }

    private void performSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { refreshTable(); return; }

        tableModel.setRowCount(0);
        List<SACatalogueItem> results = catalogueDAO.search(kw);
        for (SACatalogueItem item : results) {
            tableModel.addRow(new Object[]{
                    item.getItemId(),
                    item.getDescription(),
                    item.getPackageType(),
                    item.getUnit(),
                    item.getUnitsPerPack(),
                    String.format("%.2f", item.getCostPerUnit()),
                    String.format("%.2f", item.getPackCost()),
                    item.getAvailability()
            });
        }
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items found matching: " + kw,
                    "Search", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
