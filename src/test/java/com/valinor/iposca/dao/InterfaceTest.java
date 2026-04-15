package com.valinor.iposca.dao;

import API.CA_PU_implementation;
import API.CA_PU_interface;
import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.StockItem;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the interfaces between subsystems.
 *
 * Provided interface:
 * -getCatalogueItems, searchCatalogueItems, getCatalogueItemById
 *
 * Required interface (SA -> CA): SA_CA_interface
 *  will probs test thru connectionmanager
 *
 * These correspond to the test tables in section 7.7 of the
 * Requirements Spec doc
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InterfaceTest {

    private static CA_PU_interface caPuApi;
    private static StockDAO stockDAO;

    @BeforeAll
    static void setup() {
        DatabaseManager.initialiseDatabase();
        stockDAO = new StockDAO();

        // the CA-PU interface reads from the local SQLite database
        caPuApi = new CA_PU_implementation("ipos_ca.db");
    }

    @AfterAll
    static void cleanup() {
        DatabaseManager.closeConnection();
    }

    // PROVIDED INTERFACE TESTS: CA -> PU
    // These test the methods that IPOS-PU calls on IPOS-CA.

    @Test
    @Order(1)
    void getCatalogueItems_returnsListNotNull() {
        List<Map<String, String>> items = caPuApi.getCatalogueItems();
        assertNotNull(items, "getCatalogueItems should never return null");
    }

    @Test
    @Order(2)
    void getCatalogueItems_eachItemHasRequiredFields() {
        List<Map<String, String>> items = caPuApi.getCatalogueItems();

        // if the catalogue has been populated then check the fields
        if (!items.isEmpty()) {
            Map<String, String> first = items.get(0);
            assertTrue(first.containsKey("item_id"), "Item should have item_id");
            assertTrue(first.containsKey("description"), "Item should have description");
            assertTrue(first.containsKey("availability"), "Item should have availability");
            assertTrue(first.containsKey("cost_per_unit"), "Item should have cost_per_unit");
        }
    }

    @Test
    @Order(3)
    void searchCatalogueItems_validKeyword_returnsResults() {
        // check for crash
        List<Map<String, String>> results = caPuApi.searchCatalogueItems("Paracetamol");
        assertNotNull(results, "Search should return a list, even if empty");
    }

    @Test
    @Order(4)
    void searchCatalogueItems_nullKeyword_returnsAllItems() {
        // the implementation returns all items when keyword is null
        List<Map<String, String>> all = caPuApi.getCatalogueItems();
        List<Map<String, String>> nullSearch = caPuApi.searchCatalogueItems(null);
        assertEquals(all.size(), nullSearch.size(),
                "Null keyword should return the same as getting all items");
    }

    @Test
    @Order(5)
    void searchCatalogueItems_emptyKeyword_returnsAllItems() {
        List<Map<String, String>> all = caPuApi.getCatalogueItems();
        List<Map<String, String>> emptySearch = caPuApi.searchCatalogueItems("");
        assertEquals(all.size(), emptySearch.size(),
                "Empty keyword should return the same as getting all items");
    }

    @Test
    @Order(6)
    void getCatalogueItemById_nonExistentId_returnsNull() {
        Map<String, String> item = caPuApi.getCatalogueItemById("FAKE_ID_999");
        assertNull(item, "Non-existent item ID should return null");
    }

    // I_InventoryAPI TESTS (provided to PU)
    // These test the inventory methods from the requirements spec

    @Test
    @Order(7)
    void checkStock_validItem_returnsStockLevel() {
        // add a test item so we have something to check
        StockItem item = new StockItem("INV001", "Interface Test Item", "box",
                "Caps", 20, 5.0, 50.0, 100, 30);
        stockDAO.addStockItem(item);

        StockItem found = stockDAO.getStockItemById("INV001");
        assertNotNull(found);
        assertEquals(100, found.getAvailability(),
                "checkStock should return the correct stock level");
    }

    @Test
    @Order(8)
    void checkStock_invalidItem_returnsNull() {
        StockItem found = stockDAO.getStockItemById(null);
        assertNull(found, "Null item ID should return null");
    }

    @Test
    @Order(9)
    void reserveStock_validRequest_reducesAvailability() {
        // reduceStock acts as the reserveStock implementation
        boolean result = stockDAO.reduceStock("INV001", 5);
        assertTrue(result, "Reserving 5 of a 100-stock item should succeed");

        StockItem after = stockDAO.getStockItemById("INV001");
        assertEquals(95, after.getAvailability());
    }

    @Test
    @Order(10)
    void reserveStock_negativeQuantity_fails() {
        // trying to reduce by a negative amount
        boolean result = stockDAO.reduceStock("INV001", -1);
        assertFalse(result, "Negative quantity should be rejected");
    }

    @Test
    @Order(11)
    void reduceStock_nonExistentItem_fails() {
        boolean result = stockDAO.reduceStock("FAKE_999", 10);
        assertFalse(result, "Reducing stock for a non-existent item should fail");
    }

    @Test
    @Order(12)
    void increaseStock_validDelivery_increasesAvailability() {
        StockItem before = stockDAO.getStockItemById("INV001");
        int stockBefore = before.getAvailability();

        boolean result = stockDAO.recordDelivery("INV001", 20, "Interface test delivery");
        assertTrue(result);

        StockItem after = stockDAO.getStockItemById("INV001");
        assertEquals(stockBefore + 20, after.getAvailability(),
                "increaseStock should add to the current availability");
    }

    @Test
    @Order(13)
    void getItemDetails_validItem_returnsCorrectInfo() {
        StockItem item = stockDAO.getStockItemById("INV001");
        assertNotNull(item);
        assertEquals("Interface Test Item", item.getDescription());
        assertEquals("box", item.getPackageType());
        assertEquals(5.0, item.getBulkCost(), 0.01);
    }

    @Test
    @Order(14)
    void getItemDetails_nonExistentItem_returnsNull() {
        StockItem item = stockDAO.getStockItemById("DOESNT_EXIST_999");
        assertNull(item, "Non-existent item should return null");
    }

    // SA_CA INTERFACE TESTS (required from SA)
    // we test the local SA catalogue cache and connection manager setup instead

    @Test
    @Order(15)
    void saConnectionManager_createsWithoutError() {
        // just make sure the connection manager can be instantiated
        SAConnectionManager manager = new SAConnectionManager();
        assertNotNull(manager, "SAConnectionManager should be created without errors");
    }

    @Test
    @Order(16)
    void saCatalogueDAO_getAllItems_returnsListNotNull() {
        // tests the local cache of SA's catalogue
        SACatalogueDAO catDAO = new SACatalogueDAO();
        var items = catDAO.getAll();
        assertNotNull(items, "Local SA catalogue cache should return a list");
    }

    // cleanup anything formed by it

    @Test
    @Order(99)
    void cleanup_removeTestData() {
        // remove interface test items
        try {
            java.sql.Connection conn = DatabaseManager.getConnection();
            conn.createStatement().execute("DELETE FROM deliveries WHERE item_id = 'INV001'");
        } catch (Exception e) { /* ignore */ }
        stockDAO.deleteStockItem("INV001");

        // verify it's gone
        assertNull(stockDAO.getStockItemById("INV001"));
    }
}