package com.valinor.iposca.model;

/**
 * Represents a single item in the pharmacy's local stock.
 * Each field matches a column in the stock_items database table.
 */
public class StockItem {

    private String itemId;
    private String description;
    private String packageType;
    private String unit;
    private int unitsInPack;
    private double bulkCost;
    private double markupRate;
    private int availability;
    private int stockLimit;

    // Empty constructor for when we create a blank item to fill in later
    public StockItem() {
    }

    // Full constructor for when we have all the data at once
    public StockItem(String itemId, String description, String packageType,
                     String unit, int unitsInPack, double bulkCost,
                     double markupRate, int availability, int stockLimit) {
        this.itemId = itemId;
        this.description = description;
        this.packageType = packageType;
        this.unit = unit;
        this.unitsInPack = unitsInPack;
        this.bulkCost = bulkCost;
        this.markupRate = markupRate;
        this.availability = availability;
        this.stockLimit = stockLimit;
    }

    /**
     * Calculates the retail price by applying the markup to the bulk cost.
     * For example: bulk cost £10, markup 50% = retail price £15.
     */
    public double getRetailPrice() {
        return bulkCost * (1 + markupRate / 100.0);
    }

    /**
     * Calculates the retail price including VAT.
     * vatRate is passed in as a percentage (e.g. 20 for 20%).
     */
    public double getRetailPriceWithVAT(double vatRate) {
        return getRetailPrice() * (1 + vatRate / 100.0);
    }

    /**
     * Checks if this item's stock is below its minimum threshold.
     */
    public boolean isLowStock() {
        return availability < stockLimit;
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getUnitsInPack() {
        return unitsInPack;
    }

    public void setUnitsInPack(int unitsInPack) {
        this.unitsInPack = unitsInPack;
    }

    public double getBulkCost() {
        return bulkCost;
    }

    public void setBulkCost(double bulkCost) {
        this.bulkCost = bulkCost;
    }

    public double getMarkupRate() {
        return markupRate;
    }

    public void setMarkupRate(double markupRate) {
        this.markupRate = markupRate;
    }

    public int getAvailability() {
        return availability;
    }

    public void setAvailability(int availability) {
        this.availability = availability;
    }

    public int getStockLimit() {
        return stockLimit;
    }

    public void setStockLimit(int stockLimit) {
        this.stockLimit = stockLimit;
    }

    @Override
    public String toString() {
        return itemId + " - " + description;
    }
}
