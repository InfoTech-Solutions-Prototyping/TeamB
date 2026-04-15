package com.valinor.iposca.dao;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.ApplicationUser;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserDAO.
 * Tests user creation, authentication, role changes and deletion
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {

    private static UserDAO userDAO;
    private static int testUserId;

    @BeforeAll
    static void setup() {
        DatabaseManager.initialiseDatabase();
        userDAO = new UserDAO();
    }

    @AfterAll
    static void cleanup() {
        DatabaseManager.closeConnection();
    }

    // create tests

    @Test
    @Order(1)
    void createUser_validData_returnsPositiveId() {
        ApplicationUser user = new ApplicationUser();
        user.setUsername("testuser_junit");
        user.setPassword("testpass123");
        user.setRole("Pharmacist");

        testUserId = userDAO.createUser(user);
        assertTrue(testUserId > 0, "Creating a valid user should return a positive ID");
    }

    @Test
    @Order(2)
    void createUser_duplicateUsername_returnsNegativeOne() {
        ApplicationUser user = new ApplicationUser();
        user.setUsername("testuser_junit");
        user.setPassword("differentpass");
        user.setRole("Manager");

        int result = userDAO.createUser(user);
        assertEquals(-1, result, "Duplicate username should return -1");
    }

    // auth tests

    @Test
    @Order(3)
    void getUserFromUsername_validUsername_returnsUser() {
        ApplicationUser user = userDAO.getUserFromUsername("testuser_junit");
        assertNotNull(user, "Should find the user we just created");
        assertEquals("Pharmacist", user.getRole());
    }

    @Test
    @Order(4)
    void getUserFromUsername_nonExistent_returnsNull() {
        ApplicationUser user = userDAO.getUserFromUsername("nonexistent_user_xyz");
        assertNull(user, "Non-existent username should return null");
    }

    @Test
    @Order(5)
    void authentication_correctPassword_matches() {
        ApplicationUser user = userDAO.getUserFromUsername("testuser_junit");
        assertNotNull(user);
        assertEquals("testpass123", user.getPassword(),
                "Password should match what we stored");
    }

    @Test
    @Order(6)
    void authentication_wrongPassword_doesNotMatch() {
        ApplicationUser user = userDAO.getUserFromUsername("testuser_junit");
        assertNotNull(user);
        assertNotEquals("wrongpassword", user.getPassword(),
                "Wrong password should not match");
    }

    // role  change tests

    @Test
    @Order(7)
    void changeRole_validChange_updatesRole() {
        boolean result = userDAO.changeRole(testUserId, "Manager");
        assertTrue(result);

        ApplicationUser user = userDAO.getUserFromUsername("testuser_junit");
        assertEquals("Manager", user.getRole(), "Role should be updated to Manager");
    }

    // search tests

    @Test
    @Order(8)
    void searchUsers_byUsername_findsUser() {
        List<ApplicationUser> results = userDAO.searchUsers("testuser_junit");
        assertFalse(results.isEmpty(), "Should find user by username search");
    }

    @Test
    @Order(9)
    void getAllUsers_includesTestUser() {
        List<ApplicationUser> users = userDAO.getAllUsers();
        boolean found = users.stream().anyMatch(u -> u.getUsername().equals("testuser_junit"));
        assertTrue(found, "getAllUsers should include our test user");
    }

    // delete tests

    @Test
    @Order(10)
    void deleteUser_removesUser() {
        boolean result = userDAO.deleteUser(testUserId);
        assertTrue(result);

        ApplicationUser deleted = userDAO.getUserFromUsername("testuser_junit");
        assertNull(deleted, "User should be gone after deletion");
    }
}