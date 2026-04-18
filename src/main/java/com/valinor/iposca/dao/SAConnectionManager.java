package com.valinor.iposca.dao;

import API.SA_CA_implementation;
import API.SA_CA_interface;
import com.valinor.iposca.model.SACatalogueItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


//Wraps SA's API. Handles authentication, catalogue fetching,
//order placement, and all communication with InfoPharma.

public class SAConnectionManager {

    private final SA_CA_interface saApi;
    private String merchantId;
    private String accountStatus;
    private boolean loggedIn;

    public SAConnectionManager() {
        this.saApi = new SA_CA_implementation();
        this.loggedIn = false;
    }


    //Authenticates with SA as Cosymed.
    // Returns true if login succeeded.
    public boolean login(String username, String password) {
        try {
            System.out.println("Attempting SA login with: [" + username + "] [" + password + "]");
            Map<String, String> result = saApi.authenticateMerchant(username, password);
            System.out.println("SA login result: " + result);
            if (result != null) {
                this.merchantId = result.get("merchantId");
                this.accountStatus = result.get("accountStatus");
                this.loggedIn = true;
                return true;
            }
        } catch (Exception e) {
            System.err.println("SA login failed: " + e.getMessage());
            e.printStackTrace();
        }
        this.loggedIn = false;
        return false;
    }



    //Fetches the full catalogue from SA and converts it to our model objects.
    public List<SACatalogueItem> fetchCatalogue() {
        List<SACatalogueItem> items = new ArrayList<>();
        try {
            List<Map<String, String>> raw = saApi.getCatalogueItems();
            for (Map<String, String> map : raw) {
                SACatalogueItem item = new SACatalogueItem();
                item.setItemId(map.get("itemId"));
                item.setDescription(map.get("description"));
                item.setPackageType(map.get("packageType"));
                item.setUnit(map.get("unit"));
                item.setUnitsPerPack(Integer.parseInt(map.get("unitsPerPack")));
                item.setCostPerUnit(Double.parseDouble(map.get("costPerUnit")));
                item.setAvailability(Integer.parseInt(map.get("availability")));
                items.add(item);
            }
        } catch (Exception e) {
            System.err.println("Error fetching SA catalogue: " + e.getMessage());
        }
        return items;
    }


    //Searches SA's catalogue directly (live query, not cached).
    public List<SACatalogueItem> searchCatalogue(String keyword) {
        List<SACatalogueItem> items = new ArrayList<>();
        try {
            List<Map<String, String>> raw = saApi.searchCatalogueItems(keyword);
            for (Map<String, String> map : raw) {
                SACatalogueItem item = new SACatalogueItem();
                item.setItemId(map.get("itemId"));
                item.setDescription(map.get("description"));
                item.setPackageType(map.get("packageType"));
                item.setUnit(map.get("unit"));
                item.setUnitsPerPack(Integer.parseInt(map.get("unitsPerPack")));
                item.setCostPerUnit(Double.parseDouble(map.get("costPerUnit")));
                item.setAvailability(Integer.parseInt(map.get("availability")));
                items.add(item);
            }
        } catch (Exception e) {
            System.err.println("Error searching SA catalogue: " + e.getMessage());
        }
        return items;
    }


    //Places an order with SA
    public String placeOrder(List<Map<String, String>> orderLines) {
        if (!loggedIn || merchantId == null) {
            System.err.println("Not logged in to SA.");
            return null;
        }
        try {
            return saApi.placeOrder(merchantId, orderLines);
        } catch (Exception e) {
            System.err.println("Error placing order with SA: " + e.getMessage());
            return null;
        }
    }


    //Gets all orders for this merchant from SA.
    public List<Map<String, String>> getOrders() {
        if (!loggedIn || merchantId == null) return new ArrayList<>();
        try {
            return saApi.getOrdersByMerchant(merchantId);
        } catch (Exception e) {
            System.err.println("Error getting orders from SA: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    //Gets details for a specific order including line items.
    public Map<String, String> getOrderDetails(String orderId) {
        try {
            return saApi.getOrderDetails(orderId);
        } catch (Exception e) {
            System.err.println("Error getting order details from SA: " + e.getMessage());
            return null;
        }
    }


    //Gets all invoices for this merchant from SA.
    public List<Map<String, String>> getInvoices() {
        if (!loggedIn || merchantId == null) return new ArrayList<>();
        try {
            return saApi.getInvoicesByMerchant(merchantId);
        } catch (Exception e) {
            System.err.println("Error getting invoices from SA: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    //Gets details for a specific invoice.
    public Map<String, String> getInvoiceDetails(String invoiceId) {
        try {
            return saApi.getInvoiceDetails(invoiceId);
        } catch (Exception e) {
            System.err.println("Error getting invoice details from SA: " + e.getMessage());
            return null;
        }
    }


    //Gets the merchant's current balance, credit limit, and account status.
    public Map<String, String> getBalanceAndStatus() {
        if (!loggedIn || merchantId == null) return null;
        try {
            return saApi.getMerchantBalanceAndStatus(merchantId);
        } catch (Exception e) {
            System.err.println("Error getting balance from SA: " + e.getMessage());
            return null;
        }
    }


    public boolean isLoggedIn() { return loggedIn; }
    public String getMerchantId() { return merchantId; }
    public String getAccountStatus() { return accountStatus; }
}
