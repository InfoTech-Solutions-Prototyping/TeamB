package com.valinor.iposca.dao;

import com.valinor.iposca.db.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


// Handles all database operations for PU orders
public class PUOrderDAO {

    // Creates a new PU order with status 'Accepted'. Returns the generated order ID, or -1 on failure.
    public int createOrder(String customerName, String customerEmail, String items, String notes) {
        String sql = "INSERT INTO pu_orders (customer_name, customer_email, items, order_date, status, notes) "
                + "VALUES (?, ?, ?, ?, 'Accepted', ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customerName);
            ps.setString(2, customerEmail);
            ps.setString(3, items);
            ps.setString(4, LocalDate.now().toString());
            ps.setString(5, notes);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error creating PU order: " + e.getMessage());
        }
        return -1;
    }

    // Updates the status of an order
    public boolean updateStatus(int orderId, String newStatus) {
        String sql = "UPDATE pu_orders SET status = ? WHERE order_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating PU order status: " + e.getMessage());
        }
        return false;
    }

    // Returns all PU orders
    public List<String[]> getAllOrders() {
        List<String[]> orders = new ArrayList<>();
        String sql = "SELECT order_id, customer_name, customer_email, items, order_date, status, notes "
                + "FROM pu_orders ORDER BY order_id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                orders.add(new String[]{
                        String.valueOf(rs.getInt("order_id")),
                        rs.getString("customer_name"),
                        rs.getString("customer_email"),
                        rs.getString("items"),
                        rs.getString("order_date"),
                        rs.getString("status"),
                        rs.getString("notes")
                });
            }
        } catch (SQLException e) {
            System.err.println("Error fetching PU orders: " + e.getMessage());
        }
        return orders;
    }

    // Deletes a PU order by ID. Returns true if a row was removed.
    public boolean deleteOrder(int orderId) {
        String sql = "DELETE FROM pu_orders WHERE order_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting PU order: " + e.getMessage());
        }
        return false;
    }
}
