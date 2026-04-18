package com.valinor.iposca.dao;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.StockItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//Handles all database operations related to stock items.
public class StockDAO {

    // Adds a new stock item to the database.
    public boolean addStockItem(StockItem item) {
        String sql = "INSERT INTO stock_items (item_id, description, package_type, unit, " +
                     "units_in_pack, bulk_cost, markup_rate, availability, stock_limit) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, item.getItemId());
            pstmt.setString(2, item.getDescription());
            pstmt.setString(3, item.getPackageType());
            pstmt.setString(4, item.getUnit());
            pstmt.setInt(5, item.getUnitsInPack());
            pstmt.setDouble(6, item.getBulkCost());
            pstmt.setDouble(7, item.getMarkupRate());
            pstmt.setInt(8, item.getAvailability());
            pstmt.setInt(9, item.getStockLimit());

            pstmt.executeUpdate();
            pstmt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("Error adding stock item: " + e.getMessage());
            return false;
        }
    }

    // Updates an existing stock item in the database.
    public boolean updateStockItem(StockItem item) {
        String sql = "UPDATE stock_items SET description = ?, package_type = ?, unit = ?, " +
                     "units_in_pack = ?, bulk_cost = ?, markup_rate = ?, availability = ?, " +
                     "stock_limit = ? WHERE item_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, item.getDescription());
            pstmt.setString(2, item.getPackageType());
            pstmt.setString(3, item.getUnit());
            pstmt.setInt(4, item.getUnitsInPack());
            pstmt.setDouble(5, item.getBulkCost());
            pstmt.setDouble(6, item.getMarkupRate());
            pstmt.setInt(7, item.getAvailability());
            pstmt.setInt(8, item.getStockLimit());
            pstmt.setString(9, item.getItemId());

            pstmt.executeUpdate();
            pstmt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("Error updating stock item: " + e.getMessage());
            return false;
        }
    }

    // Deletes a stock item from the database by its ID.
    public boolean deleteStockItem(String itemId) {
        String sql = "DELETE FROM stock_items WHERE item_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, itemId);
            pstmt.executeUpdate();
            pstmt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("Error deleting stock item: " + e.getMessage());
            return false;
        }
    }

    // Gets a single stock item by its ID.
    public StockItem getStockItemById(String itemId) {
        String sql = "SELECT * FROM stock_items WHERE item_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, itemId);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                StockItem item = extractStockItemFromResultSet(rs);
                rs.close();
                pstmt.close();
                return item;
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting stock item: " + e.getMessage());
        }

        return null;
    }

    //Returns all stock items from the database.
    public List<StockItem> getAllStockItems() {
        String sql = "SELECT * FROM stock_items ORDER BY item_id";
        List<StockItem> items = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                items.add(extractStockItemFromResultSet(rs));
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting all stock items: " + e.getMessage());
        }

        return items;
    }

    //Searches stock items by keyword in the item ID or description.
    public List<StockItem> searchStockItems(String keyword) {
        String sql = "SELECT * FROM stock_items WHERE item_id LIKE ? OR description LIKE ? ORDER BY item_id";
        List<StockItem> items = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            String searchTerm = "%" + keyword + "%";
            pstmt.setString(1, searchTerm);
            pstmt.setString(2, searchTerm);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(extractStockItemFromResultSet(rs));
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error searching stock items: " + e.getMessage());
        }

        return items;
    }

    //Returns all stock items that are below their stock limit.
    public List<StockItem> getLowStockItems() {
        String sql = "SELECT * FROM stock_items WHERE availability < stock_limit ORDER BY item_id";
        List<StockItem> items = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                items.add(extractStockItemFromResultSet(rs));
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting low stock items: " + e.getMessage());
        }

        return items;
    }

    //Records a delivery and increases the stock for that item.
    //Uses a transaction so both operations happen together or neither does.
    public boolean recordDelivery(String itemId, int quantity, String notes) {
        String insertDelivery = "INSERT INTO deliveries (item_id, quantity, notes) VALUES (?, ?, ?)";
        String updateStock = "UPDATE stock_items SET availability = availability + ? WHERE item_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();

            conn.setAutoCommit(false);

            try {
                // Record the delivery
                PreparedStatement pstmt1 = conn.prepareStatement(insertDelivery);
                pstmt1.setString(1, itemId);
                pstmt1.setInt(2, quantity);
                pstmt1.setString(3, notes);
                pstmt1.executeUpdate();
                pstmt1.close();

                // Increase the stock
                PreparedStatement pstmt2 = conn.prepareStatement(updateStock);
                pstmt2.setInt(1, quantity);
                pstmt2.setString(2, itemId);
                pstmt2.executeUpdate();
                pstmt2.close();

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error recording delivery: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error with database connection: " + e.getMessage());
            return false;
        }
    }

    //Reduces stock for an item (used when a sale happens).
    //Returns false if there isn't enough stock.
    public boolean reduceStock(String itemId, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        // First check if there's enough stock
        StockItem item = getStockItemById(itemId);
        if (item == null || item.getAvailability() < quantity) {
            return false;
        }

        String sql = "UPDATE stock_items SET availability = availability - ? WHERE item_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, quantity);
            pstmt.setString(2, itemId);
            pstmt.executeUpdate();
            pstmt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("Error reducing stock: " + e.getMessage());
            return false;
        }
    }

    //Gets the current VAT rate from the system config table.
    public double getVATRate() {
        String sql = "SELECT config_value FROM system_config WHERE config_key = 'vat_rate'";

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                double rate = Double.parseDouble(rs.getString("config_value"));
                rs.close();
                stmt.close();
                return rate;
            }

            rs.close();
            stmt.close();

        } catch (SQLException | NumberFormatException e) {
            System.err.println("Error getting VAT rate: " + e.getMessage());
        }

        return 0.0;
    }

    //Updates the VAT rate in the system config.
    public boolean setVATRate(double rate) {
        String sql = "UPDATE system_config SET config_value = ? WHERE config_key = 'vat_rate'";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, String.valueOf(rate));
            pstmt.executeUpdate();
            pstmt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("Error setting VAT rate: " + e.getMessage());
            return false;
        }
    }

    //Helper method that reads one row from a database result and turns it into a StockItem object.
    //This avoids repeating the same code in every method that reads from the database.
    private StockItem extractStockItemFromResultSet(ResultSet rs) throws SQLException {
        return new StockItem(
            rs.getString("item_id"),
            rs.getString("description"),
            rs.getString("package_type"),
            rs.getString("unit"),
            rs.getInt("units_in_pack"),
            rs.getDouble("bulk_cost"),
            rs.getDouble("markup_rate"),
            rs.getInt("availability"),
            rs.getInt("stock_limit")
        );
    }
}
