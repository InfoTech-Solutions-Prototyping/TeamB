package com.valinor.iposca.dao;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.ApplicationUser;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all database operations for user accounts.
 */
public class UserDAO {

    /**
     * Creates a new user in the database.
     * Returns the generated user ID, -1 if username is taken,
     * -2 if it fails.
     */
    public int createUser(ApplicationUser user) {
        //Stop duplicate usernames
        if(getUserFromUsername(user.getUsername()) != null){
            return -1;
        }

        String sql = "INSERT INTO users (username, password, role) " +
                "VALUES (?, ?, ?)";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());

            pstmt.executeUpdate();

            // Get the auto-generated user ID
            ResultSet keys = pstmt.getGeneratedKeys();
            int newId = -1;
            if (keys.next()) {
                newId = keys.getInt(1);
            }

            keys.close();
            pstmt.close();
            return newId;

        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return -2;
        }
    }

    /**
     * Deletes a user from the database.
     */
    public boolean deleteUser(int userID) {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userID);
            pstmt.executeUpdate();
            pstmt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Changes a user's role
     */
    public boolean changeRole(int userId, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newRole);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            pstmt.close();
            return true;

        } catch (SQLException e) {
            System.err.println("Error changing role: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets a single user by their username.
     */
    public ApplicationUser getUserFromUsername(String username){
        String sql = "SELECT * FROM users WHERE username = ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                ApplicationUser user = extractUserFromResultSet(rs);
                rs.close();
                pstmt.close();
                return user;
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("User not found: " + e.getMessage());
        }

        return null;
    }

    /**
     * Returns all users from the database.
     */
    public List<ApplicationUser> getAllUsers(){
        String sql = "SELECT * FROM users ORDER BY user_id";
        List<ApplicationUser> users = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
        }

        return users;
    }

    /**
     * Searches users by username or user ID.
     */
    public List<ApplicationUser> searchUsers(String keyword){
        String sql = "SELECT * FROM users WHERE " +
                "username LIKE ? OR CAST(user_id AS TEXT) LIKE ? " +
                "ORDER BY user_id";
        List<ApplicationUser> users = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            String searchTerm = "%" + keyword + "%";
            pstmt.setString(1, searchTerm);
            pstmt.setString(2, searchTerm);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("Error searching users: " + e.getMessage());
        }

        return users;
    }

    /**
     * Helper method that reads one row from the database and turns it into a ApplicationUser object.
     */
    private ApplicationUser extractUserFromResultSet(ResultSet rs) throws SQLException {
        return new ApplicationUser(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getString("created_at")
        );
    }
}
