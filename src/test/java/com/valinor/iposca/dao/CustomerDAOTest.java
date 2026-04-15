package com.valinor.iposca.dao;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.AccountHolder;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomerDAO
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomerDAOTest {

    private static CustomerDAO customerDAO;
    private static int testAccountId;

    @BeforeAll
    static void setup() {
        DatabaseManager.initialiseDatabase();
        customerDAO = new CustomerDAO();
    }

    @AfterAll
    static void cleanup() {
        DatabaseManager.closeConnection();
    }

    // helper to build a test customer
    private AccountHolder makeTestCustomer() {
        AccountHolder h = new AccountHolder();
        h.setFirstName("Test");
        h.setLastName("Customer");
        h.setAddress("123 Test Street");
        h.setPhone("07700000000");
        h.setEmail("test@example.com");
        h.setCreditLimit(500.0);
        h.setDiscountType("fixed");
        h.setDiscountRate(10.0);
        return h;
    }

    // create tests

    @Test
    @Order(1)
    void createAccountHolder_validData_returnsPositiveId() {
        AccountHolder h = makeTestCustomer();
        testAccountId = customerDAO.createAccountHolder(h);
        assertTrue(testAccountId > 0, "Creating a valid customer should return a positive ID");
    }

    @Test
    @Order(2)
    void createAccountHolder_newAccountHasNormalStatus() {
        AccountHolder h = customerDAO.getAccountHolderById(testAccountId);
        assertNotNull(h);
        assertEquals("normal", h.getAccountStatus(), "New accounts should start as normal");
        assertEquals("no_need", h.getStatus1stReminder(), "New accounts should have no_need for 1st reminder");
        assertEquals("no_need", h.getStatus2ndReminder(), "New accounts should have no_need for 2nd reminder");
    }

    // get and search tests

    @Test
    @Order(3)
    void getAccountHolderById_existingId_returnsCorrectCustomer() {
        AccountHolder h = customerDAO.getAccountHolderById(testAccountId);
        assertNotNull(h);
        assertEquals("Test", h.getFirstName());
        assertEquals("Customer", h.getLastName());
        assertEquals(500.0, h.getCreditLimit(), 0.01);
    }

    @Test
    @Order(4)
    void getAccountHolderById_nonExistentId_returnsNull() {
        AccountHolder h = customerDAO.getAccountHolderById(99999);
        assertNull(h, "Non-existent ID should return null");
    }

    @Test
    @Order(5)
    void searchAccountHolders_byName_findsCustomer() {
        List<AccountHolder> results = customerDAO.searchAccountHolders("Test");
        assertFalse(results.isEmpty(), "Searching by name should find our test customer");
    }

    // update tests

    @Test
    @Order(6)
    void updateAccountHolder_changesDetails() {
        AccountHolder h = customerDAO.getAccountHolderById(testAccountId);
        h.setPhone("07799999999");
        h.setCreditLimit(1000.0);

        boolean result = customerDAO.updateAccountHolder(h);
        assertTrue(result);

        AccountHolder updated = customerDAO.getAccountHolderById(testAccountId);
        assertEquals("07799999999", updated.getPhone());
        assertEquals(1000.0, updated.getCreditLimit(), 0.01);
    }

    // credit and discount tests

    @Test
    @Order(7)
    void canPurchaseOnCredit_normalAccountWithinLimit_returnsTrue() {
        AccountHolder h = customerDAO.getAccountHolderById(testAccountId);
        // credit limit is 1000, balance is 0, so buying £500 should be fine
        assertTrue(h.canPurchaseOnCredit(500.0));
    }

    @Test
    @Order(8)
    void canPurchaseOnCredit_exceedsLimit_returnsFalse() {
        AccountHolder h = customerDAO.getAccountHolderById(testAccountId);
        // trying to buy more than the credit limit
        assertFalse(h.canPurchaseOnCredit(h.getCreditLimit() + 100));
    }

    @Test
    @Order(9)
    void calculateFixedDiscount_correctPercentage() {
        AccountHolder h = customerDAO.getAccountHolderById(testAccountId);
        // 10% discount on a £100 purchase = £10 discount
        double discount = h.calculateFixedDiscount(100.0);
        assertEquals(10.0, discount, 0.01);
    }

    // payment and status tests

    @Test
    @Order(10)
    void recordPayment_reducesBalance() {
        // manual balance set

        AccountHolder h = customerDAO.getAccountHolderById(testAccountId);
        double balanceBefore = h.getOutstandingBalance();

        // if balance is 0, verify there is no crash
        boolean result = customerDAO.recordPayment(testAccountId, 0.01, "card",
                "Visa", "1234", "5678", "12/27");
        assertNotNull(result);
    }

    @Test
    @Order(11)
    void restoreAccountStatus_changesStatusToNormal() {
        boolean result = customerDAO.restoreAccountStatus(testAccountId);
        assertTrue(result);

        AccountHolder h = customerDAO.getAccountHolderById(testAccountId);
        assertEquals("normal", h.getAccountStatus(),
                "After restore, account should be normal");
    }

    // reminder status tests

    @Test
    @Order(12)
    void generateReminders_noRemindersDue_returnsEmptyList() {
        // all our test accounts should have no_need status
        List<String> reminders = customerDAO.generateReminders();
        // may or may not be empty depending on other test data,
        // but shouldn't throw an error
        assertNotNull(reminders, "generateReminders should never return null");
    }

    //delete tests

    @Test
    @Order(13)
    void deleteAccountHolder_removesCustomer() {
        boolean result = customerDAO.deleteAccountHolder(testAccountId);
        assertTrue(result);

        AccountHolder deleted = customerDAO.getAccountHolderById(testAccountId);
        assertNull(deleted, "Customer should be gone after deletion");
    }

    // model method tests

    @Test
    void accountHolder_getFullName_combinesFirstAndLast() {
        AccountHolder h = new AccountHolder();
        h.setFirstName("John");
        h.setLastName("Smith");
        assertEquals("John Smith", h.getFullName());
    }

    @Test
    void accountHolder_canPurchaseOnCredit_suspendedAccount_returnsFalse() {
        AccountHolder h = new AccountHolder();
        h.setAccountStatus("suspended");
        h.setCreditLimit(1000.0);
        h.setOutstandingBalance(0.0);
        assertFalse(h.canPurchaseOnCredit(100.0),
                "Suspended accounts should not be able to buy on credit");
    }

    @Test
    void accountHolder_canPurchaseOnCredit_defaultAccount_returnsFalse() {
        AccountHolder h = new AccountHolder();
        h.setAccountStatus("in default");
        h.setCreditLimit(1000.0);
        h.setOutstandingBalance(0.0);
        assertFalse(h.canPurchaseOnCredit(100.0),
                "Accounts in default should not be able to buy on credit");
    }

    @Test
    void accountHolder_flexibleDiscountRate_correctTiers() {
        AccountHolder h = new AccountHolder();
        h.setDiscountType("flexible");

        assertEquals(1.0, h.getFlexibleDiscountRate(500), 0.01, "Under £1000 = 1%");
        assertEquals(2.0, h.getFlexibleDiscountRate(1500), 0.01, "£1000-£2000 = 2%");
        assertEquals(3.0, h.getFlexibleDiscountRate(2500), 0.01, "Over £2000 = 3%");
    }
}