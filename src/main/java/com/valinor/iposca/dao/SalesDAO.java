package com.valinor.iposca.dao;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.Sale;
import com.valinor.iposca.model.SaleItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all database operations for sales transactions.
 * Works with StockDAO to reduce stock and CustomerDAO to update balances.
 */
public class SalesDAO {

    private StockDAO stockDAO;
    private CustomerDAO customerDAO;

    public SalesDAO() {
        this.stockDAO = new StockDAO();
        this.customerDAO = new CustomerDAO();
    }

    /**
     * Records a complete sale. then saves the sale, its items, reduces stock
     * and if it's a credit sale it will add it to the customer's outstanding balance.
     */
    public int recordSale(Sale sale) {
        String insertSale = "INSERT INTO sales (account_id, subtotal, vat_amount, discount_amount, " +
                "total, payment_method, card_type, card_first_four, card_last_four, " +
                "card_expiry, is_online) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String insertItem = "INSERT INTO sale_items (sale_id, item_id, quantity, unit_price, line_total) " +
                "VALUES (?, ?, ?, ?, ?)";

        String reduceStock = "UPDATE stock_items SET availability = availability - ? WHERE item_id = ?";

        String addToBalance = "UPDATE account_holders SET outstanding_balance = outstanding_balance + ? " +
                "WHERE account_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            try {
                // save the sale header
                PreparedStatement saleStmt = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS);
                if (sale.getAccountId() != null) {
                    saleStmt.setInt(1, sale.getAccountId());
                } else {
                    saleStmt.setNull(1, Types.INTEGER);
                }
                saleStmt.setDouble(2, sale.getSubtotal());
                saleStmt.setDouble(3, sale.getVatAmount());
                saleStmt.setDouble(4, sale.getDiscountAmount());
                saleStmt.setDouble(5, sale.getTotal());
                saleStmt.setString(6, sale.getPaymentMethod());
                saleStmt.setString(7, sale.getCardType());
                saleStmt.setString(8, sale.getCardFirstFour());
                saleStmt.setString(9, sale.getCardLastFour());
                saleStmt.setString(10, sale.getCardExpiry());
                saleStmt.setInt(11, sale.isOnline() ? 1 : 0);
                saleStmt.executeUpdate();

                // grab the generated sale ID
                ResultSet keys = saleStmt.getGeneratedKeys();
                int saleId = -1;
                if (keys.next()) {
                    saleId = keys.getInt(1);
                }
                keys.close();
                saleStmt.close();

                if (saleId == -1) {
                    conn.rollback();
                    return -1;
                }

                // save each line item and reduce stock for it
                for (SaleItem item : sale.getItems()) {
                    PreparedStatement itemStmt = conn.prepareStatement(insertItem);
                    itemStmt.setInt(1, saleId);
                    itemStmt.setString(2, item.getItemId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, item.getUnitPrice());
                    itemStmt.setDouble(5, item.getLineTotal());
                    itemStmt.executeUpdate();
                    itemStmt.close();

                    // take the sold quantity out of stock
                    PreparedStatement stockStmt = conn.prepareStatement(reduceStock);
                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setString(2, item.getItemId());
                    stockStmt.executeUpdate();
                    stockStmt.close();
                }

                // if paying on credit, add the total to the customer's balance
                if ("credit".equals(sale.getPaymentMethod()) && sale.getAccountId() != null) {
                    PreparedStatement balanceStmt = conn.prepareStatement(addToBalance);
                    balanceStmt.setDouble(1, sale.getTotal());
                    balanceStmt.setInt(2, sale.getAccountId());
                    balanceStmt.executeUpdate();
                    balanceStmt.close();
                }

                conn.commit();
                return saleId;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error recording sale: " + e.getMessage());
                return -1;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Fetches a sale by its ID, including all its line items.
     */
    public Sale getSaleById(int saleId) {
        String saleSql = "SELECT * FROM sales WHERE sale_id = ?";
        String itemsSql = "SELECT si.*, s.description FROM sale_items si " +
                "JOIN stock_items s ON si.item_id = s.item_id " +
                "WHERE si.sale_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();

            // get the sale header
            PreparedStatement saleStmt = conn.prepareStatement(saleSql);
            saleStmt.setInt(1, saleId);
            ResultSet rs = saleStmt.executeQuery();

            if (!rs.next()) {
                rs.close();
                saleStmt.close();
                return null;
            }

            Sale sale = extractSaleFromResultSet(rs);
            rs.close();
            saleStmt.close();

            // get its line items
            PreparedStatement itemStmt = conn.prepareStatement(itemsSql);
            itemStmt.setInt(1, saleId);
            ResultSet itemRs = itemStmt.executeQuery();

            while (itemRs.next()) {
                SaleItem item = new SaleItem();
                item.setSaleItemId(itemRs.getInt("sale_item_id"));
                item.setSaleId(itemRs.getInt("sale_id"));
                item.setItemId(itemRs.getString("item_id"));
                item.setItemDescription(itemRs.getString("description"));
                item.setQuantity(itemRs.getInt("quantity"));
                item.setUnitPrice(itemRs.getDouble("unit_price"));
                item.setLineTotal(itemRs.getDouble("line_total"));
                sale.addItem(item);
            }

            itemRs.close();
            itemStmt.close();
            return sale;

        } catch (SQLException e) {
            System.err.println("Error getting sale: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets all sales, newest first. Doesn't load line items
     * (use getSaleById for that).
     */
    public List<Sale> getAllSales() {
        String sql = "SELECT * FROM sales ORDER BY sale_date DESC";
        List<Sale> sales = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                sales.add(extractSaleFromResultSet(rs));
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting all sales: " + e.getMessage());
        }

        return sales;
    }

    /**
     * Gets sales within a date range. Used by reports.
     */
    public List<Sale> getSalesByDateRange(String startDate, String endDate) {
        String sql = "SELECT * FROM sales WHERE sale_date BETWEEN ? AND ? ORDER BY sale_date DESC";
        List<Sale> sales = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate + " 23:59:59");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                sales.add(extractSaleFromResultSet(rs));
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting sales by date range: " + e.getMessage());
        }

        return sales;
    }

    /**
     * Builds a receipt/invoice string for a given sale.
     * Layout loosely follows Appendix 7 from the brief.
     */
    public String generateReceipt(int saleId) {
        Sale sale = getSaleById(saleId);
        if (sale == null) {
            return "Sale not found.";
        }

        // grab merchant details for the header
        String pharmacyName = getMerchantDetail("pharmacy_name");
        String address = getMerchantDetail("address");
        String phone = getMerchantDetail("phone");

        StringBuilder receipt = new StringBuilder();
        receipt.append("============================================\n");
        receipt.append("              ").append(pharmacyName).append("\n");
        receipt.append("              ").append(address).append("\n");
        receipt.append("              Phone: ").append(phone).append("\n");
        receipt.append("============================================\n");
        receipt.append("INVOICE / RECEIPT\n");
        receipt.append("Sale ID: ").append(sale.getSaleId()).append("\n");
        receipt.append("Date: ").append(sale.getSaleDate()).append("\n");

        if (sale.getAccountId() != null) {
            receipt.append("Account No: ").append(sale.getAccountId()).append("\n");
        } else {
            receipt.append("Customer: Walk-in\n");
        }

        receipt.append("--------------------------------------------\n");
        receipt.append(String.format("%-20s %5s %8s %8s\n", "Item", "Qty", "Price", "Total"));
        receipt.append("--------------------------------------------\n");

        for (SaleItem item : sale.getItems()) {
            receipt.append(String.format("%-20s %5d %8.2f %8.2f\n",
                    item.getItemDescription(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getLineTotal()));
        }

        receipt.append("--------------------------------------------\n");
        receipt.append(String.format("%-35s %8.2f\n", "Subtotal:", sale.getSubtotal()));

        if (sale.getDiscountAmount() > 0) {
            receipt.append(String.format("%-35s -%7.2f\n", "Discount:", sale.getDiscountAmount()));
        }

        receipt.append(String.format("%-35s %8.2f\n", "VAT:", sale.getVatAmount()));
        receipt.append("============================================\n");
        receipt.append(String.format("%-35s %8.2f\n", "TOTAL DUE:", sale.getTotal()));
        receipt.append("============================================\n");
        receipt.append("Payment: ").append(sale.getPaymentMethod().toUpperCase()).append("\n");

        if (sale.getCardType() != null && !sale.getCardType().isEmpty()) {
            receipt.append("Card: ").append(sale.getCardType());
            receipt.append(" ").append(sale.getCardFirstFour());
            receipt.append("****").append(sale.getCardLastFour()).append("\n");
        }

        receipt.append("\nThank you for your valued custom.\n");

        return receipt.toString();
    }

    /**
     * Gets a merchant detail from the merchant_details table.
     */
    private String getMerchantDetail(String key) {
        String sql = "SELECT detail_value FROM merchant_details WHERE detail_key = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String val = rs.getString("detail_value");
                rs.close();
                pstmt.close();
                return val;
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting merchant detail: " + e.getMessage());
        }

        return "";
    }

    /**
     * Pulls a Sale object out of a result set row.
     */
    private Sale extractSaleFromResultSet(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setSaleId(rs.getInt("sale_id"));

        int accId = rs.getInt("account_id");
        if (!rs.wasNull()) {
            sale.setAccountId(accId);
        }

        sale.setSaleDate(rs.getString("sale_date"));
        sale.setSubtotal(rs.getDouble("subtotal"));
        sale.setVatAmount(rs.getDouble("vat_amount"));
        sale.setDiscountAmount(rs.getDouble("discount_amount"));
        sale.setTotal(rs.getDouble("total"));
        sale.setPaymentMethod(rs.getString("payment_method"));
        sale.setCardType(rs.getString("card_type"));
        sale.setCardFirstFour(rs.getString("card_first_four"));
        sale.setCardLastFour(rs.getString("card_last_four"));
        sale.setCardExpiry(rs.getString("card_expiry"));
        sale.setOnline(rs.getInt("is_online") == 1);
        return sale;
    }
}