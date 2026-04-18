package com.valinor.iposca.gui;

import com.valinor.iposca.dao.CustomerDAO;
import com.valinor.iposca.dao.SalesDAO;
import com.valinor.iposca.dao.StockDAO;
import com.valinor.iposca.model.AccountHolder;
import com.valinor.iposca.model.Sale;
import com.valinor.iposca.model.SaleItem;
import com.valinor.iposca.model.StockItem;
import com.valinor.iposca.util.AppTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class SalesPanel extends JPanel {

    private SalesDAO salesDAO;
    private StockDAO stockDAO;
    private CustomerDAO customerDAO;

    private JComboBox<String> customerTypeBox;
    private JComboBox<AccountHolder> accountHolderBox;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel subtotalLabel, discountLabel, vatLabel, totalLabel;

    private JTable historyTable;
    private DefaultTableModel historyModel;

    private List<SaleItem> cartItems;

    private final String[] cartCols = {"Item ID", "Description", "Qty", "Price (£)", "Total (£)"};
    private final String[] histCols = {"Sale ID", "Date", "Customer", "Total (£)", "Payment"};

    public SalesPanel() {
        salesDAO = new SalesDAO();
        stockDAO = new StockDAO();
        customerDAO = new CustomerDAO();
        cartItems = new ArrayList<>();

        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.bg());

        add(AppTheme.headerBar("Sales"), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildNewSalePanel(), buildHistoryPanel());
        split.setDividerLocation(620);
        split.setResizeWeight(0.6);
        split.setBackground(AppTheme.bg());
        add(split, BorderLayout.CENTER);

        refreshHistory();
    }

    // left side
    private JPanel buildNewSalePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(AppTheme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 6));

        // customer selection
        JPanel custRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        custRow.setBackground(AppTheme.bg());
        JLabel custLabel = new JLabel("Customer:");
        custLabel.setFont(AppTheme.BODY);
        custLabel.setForeground(AppTheme.text());
        custRow.add(custLabel);

        customerTypeBox = new JComboBox<>(new String[]{"Walk-in", "Account Holder"});
        customerTypeBox.addActionListener(e -> accountHolderBox.setEnabled(customerTypeBox.getSelectedIndex() == 1));
        custRow.add(customerTypeBox);

        accountHolderBox = new JComboBox<>();
        accountHolderBox.setEnabled(false);
        loadAccountHolders();
        custRow.add(accountHolderBox);

        JButton refCust = AppTheme.btn("Refresh");
        refCust.addActionListener(e -> loadAccountHolders());
        custRow.add(refCust);

        panel.add(custRow, BorderLayout.NORTH);

        // cart table
        JPanel cartArea = new JPanel(new BorderLayout(5, 5));
        cartArea.setBackground(AppTheme.bg());

        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        cartTable = new JTable(cartModel);
        AppTheme.styleTable(cartTable);

        JScrollPane sp = new JScrollPane(cartTable);
        AppTheme.styleScrollPane(sp);
        cartArea.add(sp, BorderLayout.CENTER);

        // totals
        JPanel totals = new JPanel(new GridLayout(4, 1, 0, 2));
        totals.setBackground(AppTheme.surface());
        totals.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.border(), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        subtotalLabel = new JLabel("Subtotal: £0.00");
        discountLabel = new JLabel("Discount: £0.00");
        vatLabel = new JLabel("VAT: £0.00");
        totalLabel = new JLabel("TOTAL: £0.00");

        for (JLabel l : new JLabel[]{subtotalLabel, discountLabel, vatLabel, totalLabel}) {
            l.setFont(AppTheme.BODY);
            l.setForeground(AppTheme.text());
        }
        totalLabel.setFont(AppTheme.TITLE);

        totals.add(subtotalLabel);
        totals.add(discountLabel);
        totals.add(vatLabel);
        totals.add(totalLabel);
        cartArea.add(totals, BorderLayout.SOUTH);

        panel.add(cartArea, BorderLayout.CENTER);

        // cart buttons
        JPanel cartBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        cartBtns.setBackground(AppTheme.bg());

        JButton addItem = AppTheme.btn("Add Item");
        addItem.addActionListener(e -> addItemToCart());
        cartBtns.add(addItem);

        JButton rmItem = AppTheme.btn("Remove Item");
        rmItem.addActionListener(e -> removeItemFromCart());
        cartBtns.add(rmItem);

        JButton clearCart = AppTheme.btn("Clear Cart");
        clearCart.addActionListener(e -> clearCart());
        cartBtns.add(clearCart);

        JButton checkout = AppTheme.primaryBtn("Checkout");
        checkout.addActionListener(e -> processCheckout());
        cartBtns.add(checkout);

        panel.add(cartBtns, BorderLayout.SOUTH);

        return panel;
    }

    // history
    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(AppTheme.bg());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 12));

        JLabel title = new JLabel("Sales History");
        title.setFont(AppTheme.SUBTITLE);
        title.setForeground(AppTheme.text());
        panel.add(title, BorderLayout.NORTH);

        historyModel = new DefaultTableModel(histCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        historyTable = new JTable(historyModel);
        AppTheme.styleTable(historyTable);

        JScrollPane sp = new JScrollPane(historyTable);
        AppTheme.styleScrollPane(sp);
        panel.add(sp, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        btns.setBackground(AppTheme.bg());

        JButton viewBtn = AppTheme.btn("View Receipt");
        viewBtn.addActionListener(e -> viewSelectedReceipt());
        btns.add(viewBtn);

        JButton refBtn = AppTheme.btn("Refresh");
        refBtn.addActionListener(e -> refreshHistory());
        btns.add(refBtn);

        panel.add(btns, BorderLayout.SOUTH);

        return panel;
    }

    // cart

    private void loadAccountHolders() {
        accountHolderBox.removeAllItems();
        for (AccountHolder h : customerDAO.getAllAccountHolders()) {
            accountHolderBox.addItem(h);
        }
    }

    private void addItemToCart() {
        String search = JOptionPane.showInputDialog(this, "Enter item ID or name to search:");
        if (search == null || search.trim().isEmpty()) return;

        List<StockItem> results = stockDAO.searchStockItems(search.trim());
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items found.");
            return;
        }

        StockItem selected;
        if (results.size() == 1) {
            selected = results.get(0);
        } else {
            selected = (StockItem) JOptionPane.showInputDialog(this, "Select item:",
                    "Multiple Results", JOptionPane.PLAIN_MESSAGE, null,
                    results.toArray(new StockItem[0]), results.get(0));
            if (selected == null) return;
        }

        if (selected.getAvailability() <= 0) {
            JOptionPane.showMessageDialog(this, selected.getDescription() + " is out of stock.");
            return;
        }

        String qtyStr = JOptionPane.showInputDialog(this,
                selected.getDescription() + "\nAvailable: " + selected.getAvailability() +
                        "\nPrice: £" + String.format("%.2f", selected.getRetailPrice()) + "\n\nQuantity:");
        if (qtyStr == null) return;

        try {
            int qty = Integer.parseInt(qtyStr.trim());
            if (qty <= 0) { JOptionPane.showMessageDialog(this, "Must be at least 1."); return; }
            if (qty > selected.getAvailability()) {
                JOptionPane.showMessageDialog(this, "Only " + selected.getAvailability() + " available.");
                return;
            }
            cartItems.add(new SaleItem(selected.getItemId(), selected.getDescription(), qty, selected.getRetailPrice()));
            refreshCart();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a valid number.");
        }
    }

    private void removeItemFromCart() {
        int r = cartTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select an item to remove."); return; }
        cartItems.remove(r);
        refreshCart();
    }

    private void clearCart() {
        cartItems.clear();
        refreshCart();
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        double vat = stockDAO.getVATRate();
        double sub = 0;

        for (SaleItem item : cartItems) {
            cartModel.addRow(new Object[]{
                    item.getItemId(), item.getItemDescription(), item.getQuantity(),
                    String.format("%.2f", item.getUnitPrice()),
                    String.format("%.2f", item.getLineTotal())
            });
            sub += item.getLineTotal();
        }

        double discPct = 0;
        if (customerTypeBox.getSelectedIndex() == 1 && accountHolderBox.getSelectedItem() != null) {
            AccountHolder h = (AccountHolder) accountHolderBox.getSelectedItem();
            if ("fixed".equals(h.getDiscountType())) {
                discPct = h.getDiscountRate();
            } else if ("flexible".equals(h.getDiscountType())) {
                discPct = customerDAO.getVariableDiscountRate(h.getAccountId(), sub);
            }
        }

        double disc = sub * (discPct / 100.0);
        double afterDisc = sub - disc;
        double vatAmt = afterDisc * (vat / 100.0);
        double total = afterDisc + vatAmt;

        subtotalLabel.setText(String.format("Subtotal: £%.2f", sub));
        discountLabel.setText(String.format("Discount: £%.2f (%.1f%%)", disc, discPct));
        vatLabel.setText(String.format("VAT: £%.2f (%.1f%%)", vatAmt, vat));
        totalLabel.setText(String.format("TOTAL: £%.2f", total));
    }

    // checkout

    private void processCheckout() {
        if (cartItems.isEmpty()) { JOptionPane.showMessageDialog(this, "Cart is empty."); return; }

        double vat = stockDAO.getVATRate();
        boolean isAccHolder = customerTypeBox.getSelectedIndex() == 1;
        AccountHolder holder = isAccHolder ? (AccountHolder) accountHolderBox.getSelectedItem() : null;

        if (isAccHolder && holder == null) { JOptionPane.showMessageDialog(this, "Select an account holder."); return; }

        double sub = 0;
        for (SaleItem item : cartItems) sub += item.getLineTotal();

        double discPct = 0;
        if (holder != null) {
            if ("fixed".equals(holder.getDiscountType())) {
                discPct = holder.getDiscountRate();
            } else if ("flexible".equals(holder.getDiscountType())) {
                discPct = customerDAO.getVariableDiscountRate(holder.getAccountId(), sub);
            }
        }
        double disc = sub * (discPct / 100.0);
        double afterDisc = sub - disc;
        double vatAmt = afterDisc * (vat / 100.0);
        double total = afterDisc + vatAmt;

        // payment options based on customer type
        String[] options = isAccHolder
                ? new String[]{"Card", "Credit (add to balance)"}
                : new String[]{"Cash", "Card"};

        String choice = (String) JOptionPane.showInputDialog(this,
                String.format("Total: £%.2f\n\nPayment method:", total),
                "Payment", JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == null) return;

        String method = choice.startsWith("Credit") ? "credit" : choice.equals("Card") ? "card" : "cash";

        // credit checks
        if ("credit".equals(method) && holder != null) {
            if (!"normal".equals(holder.getAccountStatus())) {
                JOptionPane.showMessageDialog(this, "Account is " + holder.getAccountStatus() + ". Cannot use credit.");
                return;
            }
            if (!holder.canPurchaseOnCredit(total)) {
                JOptionPane.showMessageDialog(this, "Credit limit would be exceeded.");
                return;
            }
        }

        // card details
        String cardType = null, cardF4 = null, cardL4 = null, cardExp = null;
        if ("card".equals(method)) {
            JTextField ctF = new JTextField(10), cfF = new JTextField(4),
                    clF = new JTextField(4), ceF = new JTextField(7);
            JPanel cp = new JPanel(new GridLayout(4, 2, 5, 5));
            cp.add(new JLabel("Card Type:")); cp.add(ctF);
            cp.add(new JLabel("First 4:")); cp.add(cfF);
            cp.add(new JLabel("Last 4:")); cp.add(clF);
            cp.add(new JLabel("Expiry (MM/YY):")); cp.add(ceF);

            if (JOptionPane.showConfirmDialog(this, cp, "Card Details",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

            cardType = ctF.getText().trim();
            cardF4 = cfF.getText().trim();
            cardL4 = clF.getText().trim();
            cardExp = ceF.getText().trim();
            if (cardType.isEmpty() || cardF4.isEmpty() || cardL4.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill in all card details.");
                return;
            }
        }

        // build and save the sale
        Sale sale = new Sale();
        sale.setAccountId(holder != null ? holder.getAccountId() : null);
        sale.setSubtotal(sub);
        sale.setDiscountAmount(disc);
        sale.setVatAmount(vatAmt);
        sale.setTotal(total);
        sale.setPaymentMethod(method);
        sale.setCardType(cardType);
        sale.setCardFirstFour(cardF4);
        sale.setCardLastFour(cardL4);
        sale.setCardExpiry(cardExp);
        sale.setOnline(false);
        sale.setItems(new ArrayList<>(cartItems));

        int saleId = salesDAO.recordSale(sale);
        if (saleId > 0) {
            String receipt = salesDAO.generateReceipt(saleId);
            JTextArea ta = new JTextArea(receipt);
            ta.setFont(AppTheme.MONO);
            ta.setEditable(false);
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(450, 380));
            JOptionPane.showMessageDialog(this, sp, "Receipt #" + saleId, JOptionPane.INFORMATION_MESSAGE);

            clearCart();
            refreshHistory();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to record sale.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // history
    private void viewSelectedReceipt() {
        int r = historyTable.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select a sale."); return; }

        int saleId = (int) historyModel.getValueAt(r, 0);
        String receipt = salesDAO.generateReceipt(saleId);

        JTextArea ta = new JTextArea(receipt);
        ta.setFont(AppTheme.MONO);
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(450, 380));
        JOptionPane.showMessageDialog(this, sp, "Receipt #" + saleId, JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        for (Sale sale : salesDAO.getAllSales()) {
            String cust;
            if (sale.getAccountId() != null) {
                AccountHolder h = customerDAO.getAccountHolderById(sale.getAccountId());
                cust = h != null ? h.getFullName() : "Account #" + sale.getAccountId();
            } else {
                cust = "Walk-in";
            }
            historyModel.addRow(new Object[]{
                    sale.getSaleId(), sale.getSaleDate(), cust,
                    String.format("%.2f", sale.getTotal()), sale.getPaymentMethod()
            });
        }
    }
}