package com.valinor.iposca.model;

import java.util.ArrayList;
import java.util.List;


 //Represents a sale transaction made at the pharmacy.

public class Sale {

    private int saleId;
    private Integer accountId;  // null for occasional customers
    private String saleDate;
    private double subtotal;
    private double vatAmount;
    private double discountAmount;
    private double total;
    private String paymentMethod;  // "cash", "card", or "credit"
    private String cardType;
    private String cardFirstFour;
    private String cardLastFour;
    private String cardExpiry;
    private boolean isOnline;

    // the individual items in this sale
    private List<SaleItem> items;

    public Sale() {
        this.items = new ArrayList<>();
    }

    // adds an item to this sale
    public void addItem(SaleItem item) {
        items.add(item);
    }

    // works out subtotal from all the line items
    public void calculateSubtotal() {
        subtotal = 0;
        for (SaleItem item : items) {
            subtotal += item.getLineTotal();
        }
    }

    // works out VAT and total from the subtotal
    public void calculateTotal(double vatRate, double discountPercent) {
        discountAmount = subtotal * (discountPercent / 100.0);
        double afterDiscount = subtotal - discountAmount;
        vatAmount = afterDiscount * (vatRate / 100.0);
        total = afterDiscount + vatAmount;
    }

    // checks if this sale is for an account holder
    public boolean isAccountHolderSale() {
        return accountId != null;
    }


    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public String getSaleDate() { return saleDate; }
    public void setSaleDate(String saleDate) { this.saleDate = saleDate; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getVatAmount() { return vatAmount; }
    public void setVatAmount(double vatAmount) { this.vatAmount = vatAmount; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getCardFirstFour() { return cardFirstFour; }
    public void setCardFirstFour(String cardFirstFour) { this.cardFirstFour = cardFirstFour; }

    public String getCardLastFour() { return cardLastFour; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }

    public String getCardExpiry() { return cardExpiry; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
}