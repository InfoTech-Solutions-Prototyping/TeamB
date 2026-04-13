package com.valinor.iposca.model;

/**
 * Represents a customer account holder in the pharmacy.
 * Account holders can buy on credit, receive discounts, and get payment reminders.
 * Each field matches a column in the account_holders database table.
 */
public class AccountHolder {

    private int accountId;
    private String firstName;
    private String lastName;
    private String address;
    private String phone;
    private String email;
    private double creditLimit;
    private double outstandingBalance;
    private String discountType;     // "none", "fixed", or "flexible"
    private double discountRate;     // percentage for fixed discount
    private String accountStatus;    // "normal", "suspended", or "in default"
    private String status1stReminder; // "no_need", "due", or "sent"
    private String status2ndReminder; // "no_need", "due", or "sent"
    private String date1stReminder;
    private String date2ndReminder;
    private String createdAt;

    // Empty constructor
    public AccountHolder() {
    }

    /**
     * Returns the customer's full name.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Checks if the customer can make purchases on credit.
     * They can only buy on credit if their account is "normal" and
     * adding the new amount wouldn't exceed their credit limit.
     */
    public boolean canPurchaseOnCredit(double amount) {
        if (!"normal".equals(accountStatus)) {
            return false;
        }
        return (outstandingBalance + amount) <= creditLimit;
    }

    /**
     * Calculates the fixed discount amount for a given purchase total.
     * Only applies if the discount type is "fixed".
     */
    public double calculateFixedDiscount(double purchaseTotal) {
        if ("fixed".equals(discountType)) {
            return purchaseTotal * (discountRate / 100.0);
        }
        return 0.0;
    }

    /**
     * Calculates the flexible discount rate based on total monthly spending.
     * The brief says: 1% for under £1000, 2% for £1000-£2000, 3% for over £2000.
     */
    public double getFlexibleDiscountRate(double monthlyTotal) {
        if (!"flexible".equals(discountType)) {
            return 0.0;
        }
        if (monthlyTotal > 2000) {
            return 3.0;
        } else if (monthlyTotal >= 1000) {
            return 2.0;
        } else {
            return 1.0;
        }
    }

    // ==================== GETTERS AND SETTERS ====================

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(double creditLimit) {
        this.creditLimit = creditLimit;
    }

    public double getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(double outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(double discountRate) {
        this.discountRate = discountRate;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getStatus1stReminder() {
        return status1stReminder;
    }

    public void setStatus1stReminder(String status1stReminder) {
        this.status1stReminder = status1stReminder;
    }

    public String getStatus2ndReminder() {
        return status2ndReminder;
    }

    public void setStatus2ndReminder(String status2ndReminder) {
        this.status2ndReminder = status2ndReminder;
    }

    public String getDate1stReminder() {
        return date1stReminder;
    }

    public void setDate1stReminder(String date1stReminder) {
        this.date1stReminder = date1stReminder;
    }

    public String getDate2ndReminder() {
        return date2ndReminder;
    }

    public void setDate2ndReminder(String date2ndReminder) {
        this.date2ndReminder = date2ndReminder;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return accountId + " - " + getFullName();
    }
}
