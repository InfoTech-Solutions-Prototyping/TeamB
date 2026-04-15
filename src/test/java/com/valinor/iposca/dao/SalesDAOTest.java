package com.valinor.iposca.dao;

import com.valinor.iposca.db.DatabaseManager;
import com.valinor.iposca.model.Sale;
import com.valinor.iposca.model.SaleItem;
import com.valinor.iposca.model.StockItem;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SalesDAO.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SalesDAOTest {

    private static SalesDAO salesDAO;
    private static StockDAO stockDAO;
    private static int testSaleId;

    @BeforeAll
    static void setup() {
        DatabaseManager.initialiseDatabase();
        salesDAO = new SalesDAO();
        stockDAO = new StockDAO();

        // make sure we have a stock item to sell
        StockItem item = new StockItem("SALE001", "Test Sale Item", "box", "Caps",
                20, 5.0, 100.0, 200, 50);
        stockDAO.addStockItem(item);
    }

    @AfterAll
    static void cleanup() {
        // tidy up test data
        stockDAO.deleteStockItem("SALE001");
        DatabaseManager.closeConnection();
    }

    // helper to build a simple cash sale for a walk-in customer
    private Sale makeTestSale() {
        Sale sale = new Sale();
        sale.setAccountId(null);  // walk-in
        sale.setSubtotal(20.0);
        sale.setVatAmount(4.0);
        sale.setDiscountAmount(0.0);
        sale.setTotal(24.0);
        sale.setPaymentMethod("cash");
        sale.setOnline(false);

        SaleItem item = new SaleItem("SALE001", "Test Sale Item", 2, 10.0);
        sale.addItem(item);

        return sale;
    }

    // RECORD SALE TESTS

    @Test
    @Order(1)
    void recordSale_validSale_returnsPositiveId() {
        Sale sale = makeTestSale();
        testSaleId = salesDAO.recordSale(sale);
        assertTrue(testSaleId > 0, "Recording a valid sale should return a positive ID");
    }

    @Test
    @Order(2)
    void recordSale_reducesStockForSoldItems() {
        StockItem item = stockDAO.getStockItemById("SALE001");
        assertNotNull(item);
        assertTrue(item.getAvailability() < 200,
                "Stock should be reduced after a sale");
    }

    @Test
    @Order(3)
    void recordSale_emptySale_stillRecords() {
        Sale emptySale = new Sale();
        emptySale.setSubtotal(0);
        emptySale.setVatAmount(0);
        emptySale.setDiscountAmount(0);
        emptySale.setTotal(0);
        emptySale.setPaymentMethod("cash");
        emptySale.setOnline(false);

        // a sale with no items - should still get an ID
        int id = salesDAO.recordSale(emptySale);
        assertTrue(id > 0, "Even an empty sale should be recorded");
    }

    //  RETRIEVE SALE TESTS

    @Test
    @Order(4)
    void getSaleById_existingSale_returnsSaleWithItems() {
        Sale sale = salesDAO.getSaleById(testSaleId);
        assertNotNull(sale, "Should find the sale we recorded");
        assertEquals(24.0, sale.getTotal(), 0.01);
        assertEquals("cash", sale.getPaymentMethod());
        assertFalse(sale.getItems().isEmpty(), "Sale should have line items");
    }

    @Test
    @Order(5)
    void getSaleById_nonExistentId_returnsNull() {
        Sale sale = salesDAO.getSaleById(99999);
        assertNull(sale, "Non-existent sale ID should return null");
    }

    @Test
    @Order(6)
    void getAllSales_includesTestSale() {
        List<Sale> sales = salesDAO.getAllSales();
        boolean found = sales.stream().anyMatch(s -> s.getSaleId() == testSaleId);
        assertTrue(found, "getAllSales should include our test sale");
    }

    //RECEIPT TESTS

    @Test
    @Order(7)
    void generateReceipt_validSale_containsExpectedInfo() {
        String receipt = salesDAO.generateReceipt(testSaleId);
        assertNotNull(receipt);
        assertTrue(receipt.contains("INVOICE"), "Receipt should contain INVOICE heading");
        assertTrue(receipt.contains("Test Sale Item"), "Receipt should contain the item name");
        assertTrue(receipt.contains("24.00"), "Receipt should show the total");
        assertTrue(receipt.contains("CASH"), "Receipt should show payment method");
    }

    @Test
    @Order(8)
    void generateReceipt_nonExistentSale_returnsErrorMessage() {
        String receipt = salesDAO.generateReceipt(99999);
        assertTrue(receipt.contains("not found"), "Should return an error for non-existent sale");
    }

    // DATE RANGE TESTS

    @Test
    @Order(9)
    void getSalesByDateRange_todaysDate_findsSales() {
        String today = java.time.LocalDate.now().toString();
        List<Sale> sales = salesDAO.getSalesByDateRange(today, today);
        assertFalse(sales.isEmpty(), "There should be sales recorded today");
    }

    @Test
    @Order(10)
    void getSalesByDateRange_futureDate_returnsEmpty() {
        List<Sale> sales = salesDAO.getSalesByDateRange("2099-01-01", "2099-12-31");
        assertTrue(sales.isEmpty(), "No sales should exist in the year 2099");
    }

    // MODEL TESTS

    @Test
    void sale_calculateSubtotal_sumsLineItems() {
        Sale sale = new Sale();
        sale.addItem(new SaleItem("A", "Item A", 2, 10.0));
        sale.addItem(new SaleItem("B", "Item B", 3, 5.0));

        sale.calculateSubtotal();
        assertEquals(35.0, sale.getSubtotal(), 0.01,
                "2*10 + 3*5 = 35");
    }

    @Test
    void sale_calculateTotal_appliesDiscountAndVAT() {
        Sale sale = new Sale();
        sale.addItem(new SaleItem("A", "Item A", 1, 100.0));
        sale.calculateSubtotal();

        // 10% discount, 20% VAT
        sale.calculateTotal(20.0, 10.0);

        assertEquals(10.0, sale.getDiscountAmount(), 0.01, "10% of £100 = £10");
        assertEquals(18.0, sale.getVatAmount(), 0.01, "20% of £90 = £18");
        assertEquals(108.0, sale.getTotal(), 0.01, "£90 + £18 = £108");
    }

    @Test
    void saleItem_recalculate_updatesLineTotal() {
        SaleItem item = new SaleItem("X", "Test", 5, 10.0);
        assertEquals(50.0, item.getLineTotal(), 0.01);

        item.setQuantity(3);
        item.recalculate();
        assertEquals(30.0, item.getLineTotal(), 0.01);
    }
}