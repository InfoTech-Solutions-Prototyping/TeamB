package com.valinor.iposca.dao;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.StockItem;
import org.junit.jupiter.api.*;
import java.sql.Connection;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StockDAO
 * Each test uses a fresh database so results don't affect each other
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockDAOTest {

    private static StockDAO stockDAO;

    @BeforeAll
    static void setup() {
        DatabaseManager.initialiseDatabase();
        stockDAO = new StockDAO();

        // clean up leftover test data - delete deliveries first due to foreign keys error
        try {
            Connection conn = DatabaseManager.getConnection();
            conn.createStatement().execute("DELETE FROM deliveries WHERE item_id IN ('TEST001', 'LOW001', 'DEL001')");
            conn.createStatement().execute("DELETE FROM sale_items WHERE item_id IN ('TEST001', 'LOW001', 'DEL001')");
        } catch (Exception e) {
            // ignore if tables don't exist yet
        }
        stockDAO.deleteStockItem("TEST001");
        stockDAO.deleteStockItem("LOW001");
        stockDAO.deleteStockItem("DEL001");
    }

    @AfterAll
    static void cleanup() {
        DatabaseManager.closeConnection();
        // delete the test database file
        new java.io.File("ipos_ca_test.db").delete();
    }

    // helper to make a test stock item
    private StockItem makeTestItem(String id, String name, int availability, int limit) {
        return new StockItem(id, name, "box", "Caps", 20,
                10.0, 50.0, availability, limit);
    }

    //add tests

    @Test
    @Order(1)
    void addStockItem_validItem_returnsTrue() {
        StockItem item = makeTestItem("TEST001", "Test Paracetamol", 500, 100);
        boolean result = stockDAO.addStockItem(item);
        assertTrue(result, "Adding a valid stock item should return true");
    }

    @Test
    @Order(2)
    void addStockItem_duplicateId_returnsFalse() {
        StockItem item = makeTestItem("TEST001", "Duplicate Item", 100, 50);
        boolean result = stockDAO.addStockItem(item);
        assertFalse(result, "Adding an item with a duplicate ID should fail");
    }

    // get tests

    @Test
    @Order(3)
    void getStockItemById_existingItem_returnsItem() {
        StockItem item = stockDAO.getStockItemById("TEST001");
        assertNotNull(item, "Should find the item we just added");
        assertEquals("Test Paracetamol", item.getDescription());
        assertEquals(500, item.getAvailability());
    }

    @Test
    @Order(4)
    void getStockItemById_nonExistentItem_returnsNull() {
        StockItem item = stockDAO.getStockItemById("FAKE999");
        assertNull(item, "Looking up a non-existent item should return null");
    }

    // update tests

    @Test
    @Order(5)
    void updateStockItem_validUpdate_changesData() {
        StockItem item = stockDAO.getStockItemById("TEST001");
        item.setDescription("Updated Paracetamol");
        item.setAvailability(999);

        boolean result = stockDAO.updateStockItem(item);
        assertTrue(result);

        // check it actually changed
        StockItem updated = stockDAO.getStockItemById("TEST001");
        assertEquals("Updated Paracetamol", updated.getDescription());
        assertEquals(999, updated.getAvailability());
    }

    // search tests

    @Test
    @Order(6)
    void searchStockItems_matchingKeyword_returnsResults() {
        List<StockItem> results = stockDAO.searchStockItems("Paracetamol");
        assertFalse(results.isEmpty(), "Search for 'Paracetamol' should find our test item");
    }

    @Test
    @Order(7)
    void searchStockItems_noMatch_returnsEmptyList() {
        List<StockItem> results = stockDAO.searchStockItems("xyznonexistent");
        assertTrue(results.isEmpty(), "Searching for something that doesn't exist should return empty");
    }

    // delivery tests

    @Test
    @Order(8)
    void recordDelivery_validDelivery_increasesStock() {
        // get current stock level
        StockItem before = stockDAO.getStockItemById("TEST001");
        int stockBefore = before.getAvailability();

        boolean result = stockDAO.recordDelivery("TEST001", 50, "Test delivery");
        assertTrue(result);

        // check stock went up by 50
        StockItem after = stockDAO.getStockItemById("TEST001");
        assertEquals(stockBefore + 50, after.getAvailability(),
                "Stock should increase by the delivery quantity");
    }

    // reduce stock tests

    @Test
    @Order(9)
    void reduceStock_sufficientStock_reducesAndReturnsTrue() {
        StockItem before = stockDAO.getStockItemById("TEST001");
        int stockBefore = before.getAvailability();

        boolean result = stockDAO.reduceStock("TEST001", 10);
        assertTrue(result);

        StockItem after = stockDAO.getStockItemById("TEST001");
        assertEquals(stockBefore - 10, after.getAvailability());
    }

    @Test
    @Order(10)
    void reduceStock_insufficientStock_returnsFalse() {
        // try to reduce by more than whats available
        StockItem item = stockDAO.getStockItemById("TEST001");
        boolean result = stockDAO.reduceStock("TEST001", item.getAvailability() + 1000);
        assertFalse(result, "Reducing more than available stock should fail");
    }

    @Test
    @Order(11)
    void reduceStock_nonExistentItem_returnsFalse() {
        boolean result = stockDAO.reduceStock("FAKE999", 10);
        assertFalse(result, "Reducing stock for a non-existent item should fail");
    }

    // low stock tests

    @Test
    @Order(12)
    void getLowStockItems_itemBelowLimit_appearsInList() {
        // add an item that's below its stock limit
        StockItem lowItem = makeTestItem("LOW001", "Low Stock Item", 5, 100);
        stockDAO.addStockItem(lowItem);

        List<StockItem> lowItems = stockDAO.getLowStockItems();
        boolean found = lowItems.stream().anyMatch(i -> i.getItemId().equals("LOW001"));
        assertTrue(found, "Item below its stock limit should appear in low stock list");
    }

    //vat tests

    @Test
    @Order(13)
    void setAndGetVATRate_setsCorrectly() {
        stockDAO.setVATRate(20.0);
        double rate = stockDAO.getVATRate();
        assertEquals(20.0, rate, 0.01, "VAT rate should be 20% after setting it");
    }

    // delete tests

    @Test
    @Order(14)
    void deleteStockItem_existingItem_removesIt() {
        // uses a separate item with nothing linked
        StockItem fresh = makeTestItem("DEL001", "Delete Test Item", 10, 5);
        stockDAO.addStockItem(fresh);

        boolean result = stockDAO.deleteStockItem("DEL001");
        assertTrue(result);

        StockItem deleted = stockDAO.getStockItemById("DEL001");
        assertNull(deleted, "Item should be gone after deletion");
    }

    // model method tests

    @Test
    void stockItem_getRetailPrice_appliesMarkupCorrectly() {
        // bulk cost £10, markup 50% = retail £15
        StockItem item = makeTestItem("X", "X", 100, 10);
        item.setBulkCost(10.0);
        item.setMarkupRate(50.0);

        assertEquals(15.0, item.getRetailPrice(), 0.01,
                "£10 bulk cost with 50% markup should give £15 retail");
    }

    @Test
    void stockItem_getRetailPriceWithVAT_appliesVATCorrectly() {
        StockItem item = makeTestItem("X", "X", 100, 10);
        item.setBulkCost(10.0);
        item.setMarkupRate(50.0);

        // retail £15 + 20% VAT = £18
        assertEquals(18.0, item.getRetailPriceWithVAT(20.0), 0.01,
                "£15 retail with 20% VAT should give £18");
    }

    @Test
    void stockItem_isLowStock_returnsTrueWhenBelowLimit() {
        StockItem low = makeTestItem("X", "X", 5, 100);
        assertTrue(low.isLowStock(), "5 available with limit 100 should be low stock");

        StockItem ok = makeTestItem("X", "X", 200, 100);
        assertFalse(ok.isLowStock(), "200 available with limit 100 should not be low stock");
    }
}