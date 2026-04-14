package com.valinor.iposca.model;


 //Represents a single item from the SA (InfoPharma) catalogue.
 //This is the wholesale catalogue — costs are what CA pays, not what customers pay.

public class SACatalogueItem {

    private String itemId;
    private String description;
    private String packageType;
    private String unit;
    private int unitsPerPack;
    private double costPerUnit;
    private int availability;
    private String lastSynced;

    public SACatalogueItem() {
    }

    public SACatalogueItem(String itemId, String description, String packageType,
                           String unit, int unitsPerPack, double costPerUnit,
                           int availability, String lastSynced) {
        this.itemId = itemId;
        this.description = description;
        this.packageType = packageType;
        this.unit = unit;
        this.unitsPerPack = unitsPerPack;
        this.costPerUnit = costPerUnit;
        this.availability = availability;
        this.lastSynced = lastSynced;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPackageType() { return packageType; }
    public void setPackageType(String packageType) { this.packageType = packageType; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getUnitsPerPack() { return unitsPerPack; }
    public void setUnitsPerPack(int unitsPerPack) { this.unitsPerPack = unitsPerPack; }

    public double getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(double costPerUnit) { this.costPerUnit = costPerUnit; }

    public int getAvailability() { return availability; }
    public void setAvailability(int availability) { this.availability = availability; }

    public String getLastSynced() { return lastSynced; }
    public void setLastSynced(String lastSynced) { this.lastSynced = lastSynced; }


     //Total cost for one pack (cost per unit * units in pack).

    public double getPackCost() {
        return costPerUnit * unitsPerPack;
    }

    @Override
    public String toString() {
        return itemId + " - " + description;
    }
}
