package com.valinor.iposca.dao;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.SACatalogueItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the local SQLite cache of SA's catalogue.
 * Data is fetched from SA and stored here so we can browse it offline.
 */
public class SACatalogueDAO {

    /**
     * Clears the local cache and inserts all items fresh from SA.
     * Called after fetching the catalogue from SA.
     */
    public boolean replaceAll(List<SACatalogueItem> items) {
        String deleteSql = "DELETE FROM sa_catalogue";
        String insertSql = "INSERT INTO sa_catalogue (item_id, description, package_type, " +
                "unit, units_per_pack, cost_per_unit, availability) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            Connection conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            try {
                conn.createStatement().execute(deleteSql);

                PreparedStatement pstmt = conn.prepareStatement(insertSql);
                for (SACatalogueItem item : items) {
                    pstmt.setString(1, item.getItemId());
                    pstmt.setString(2, item.getDescription());
                    pstmt.setString(3, item.getPackageType());
                    pstmt.setString(4, item.getUnit());
                    pstmt.setInt(5, item.getUnitsPerPack());
                    pstmt.setDouble(6, item.getCostPerUnit());
                    pstmt.setInt(7, item.getAvailability());
                    pstmt.executeUpdate();
                }
                pstmt.close();

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error replacing SA catalogue: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error with database connection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns all cached SA catalogue items.
     */
    public List<SACatalogueItem> getAll() {
        String sql = "SELECT * FROM sa_catalogue ORDER BY item_id";
        List<SACatalogueItem> items = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                items.add(extractItem(rs));
            }
            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting SA catalogue: " + e.getMessage());
        }

        return items;
    }

    /**
     * Searches the cached catalogue by keyword in item ID or description.
     */
    public List<SACatalogueItem> search(String keyword) {
        String sql = "SELECT * FROM sa_catalogue WHERE item_id LIKE ? OR description LIKE ? ORDER BY item_id";
        List<SACatalogueItem> items = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            String term = "%" + keyword + "%";
            pstmt.setString(1, term);
            pstmt.setString(2, term);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                items.add(extractItem(rs));
            }
            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error searching SA catalogue: " + e.getMessage());
        }

        return items;
    }

    /**
     * Gets a single cached item by ID.
     */
    public SACatalogueItem getById(String itemId) {
        String sql = "SELECT * FROM sa_catalogue WHERE item_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, itemId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                SACatalogueItem item = extractItem(rs);
                rs.close();
                pstmt.close();
                return item;
            }
            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting SA catalogue item: " + e.getMessage());
        }

        return null;
    }

    private SACatalogueItem extractItem(ResultSet rs) throws SQLException {
        return new SACatalogueItem(
                rs.getString("item_id"),
                rs.getString("description"),
                rs.getString("package_type"),
                rs.getString("unit"),
                rs.getInt("units_per_pack"),
                rs.getDouble("cost_per_unit"),
                rs.getInt("availability"),
                rs.getString("last_synced")
        );
    }
}
