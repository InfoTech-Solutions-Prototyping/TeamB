package com.valinor.iposca.model;

/**
 * One line item in a sale - e.g. "3 packs of Paracetamol at £0.15 each".
 * A Sale contains one or more of these.
 */
public class SaleItem {

    private int saleItemId;
    private int saleId;
    private String itemId;
    private String itemDescription; // not stored in DB, just for display
    private int quantity;
    private double unitPrice;
    private double lineTotal;

    public SaleItem() {
    }

    public SaleItem(String itemId, String itemDescription, int quantity, double unitPrice) {
        this.itemId = itemId;
        this.itemDescription = itemDescription;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = quantity * unitPrice;
    }

    // recalculates the line total from qty and price
    public void recalculate() {
        this.lineTotal = quantity * unitPrice;
    }

    // ==================== GETTERS AND SETTERS ====================

    public int getSaleItemId() { return saleItemId; }
    public void setSaleItemId(int saleItemId) { this.saleItemId = saleItemId; }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
}