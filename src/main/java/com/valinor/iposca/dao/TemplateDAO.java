package com.valinor.iposca.dao;

import com.valinor.iposca.db.DatabaseManager;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class TemplateDAO {

    public Map<String, String> getAllDetails() {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT detail_key, detail_value FROM merchant_details";

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                map.put(rs.getString("detail_key"), rs.getString("detail_value"));
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error loading merchant details: " + e.getMessage());
        }

        return map;
    }

    public String getDetail(String key) {
        String sql = "SELECT detail_value FROM merchant_details WHERE detail_key = ?";
        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String value = rs.getString("detail_value");
                rs.close();
                pstmt.close();
                return value;
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("Error getting detail: " + e.getMessage());
        }
        return "";
    }

    public boolean setDetail(String key, String value) {
        String sql = "INSERT INTO merchant_details(detail_key, detail_value) VALUES(?, ?) " +
                "ON CONFLICT(detail_key) DO UPDATE SET detail_value = excluded.detail_value";
        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
            pstmt.close();
            return true;
        } catch (SQLException e) {
            System.err.println("Error saving detail: " + e.getMessage());
            return false;
        }
    }

}