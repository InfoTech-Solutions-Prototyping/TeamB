package com.valinor.iposca.dao;

import com.valinor.iposca.model.AccountHolder;
import com.valinor.iposca.model.Sale;
import com.valinor.iposca.model.StockItem;

import java.util.List;

public class ReportDAO {

    private final SalesDAO salesDAO = new SalesDAO();
    private final StockDAO stockDAO = new StockDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    public String generateSalesReport(String startDate, String endDate) {
        List<Sale> sales = salesDAO.getSalesByDateRange(startDate, endDate);

        StringBuilder sb = new StringBuilder();
        sb.append("SALES REPORT\n");
        sb.append("From ").append(startDate).append(" to ").append(endDate).append("\n\n");

        double totalRevenue = 0;
        double totalVat = 0;
        double totalDiscount = 0;

        sb.append(String.format("%-8s %-20s %-12s %-10s\n", "Sale ID", "Date", "Method", "Total"));
        sb.append("--------------------------------------------------------------\n");

        for (Sale s : sales) {
            sb.append(String.format("%-8d %-20s %-12s £%-10.2f\n",
                    s.getSaleId(),
                    s.getSaleDate(),
                    s.getPaymentMethod(),
                    s.getTotal()));

            totalRevenue += s.getTotal();
            totalVat += s.getVatAmount();
            totalDiscount += s.getDiscountAmount();
        }

        sb.append("\n");
        sb.append(String.format("Number of sales: %d\n", sales.size()));
        sb.append(String.format("Total revenue: £%.2f\n", totalRevenue));
        sb.append(String.format("Total VAT: £%.2f\n", totalVat));
        sb.append(String.format("Total discount: £%.2f\n", totalDiscount));

        return sb.toString();
    }

    public String generateLowStockReport() {
        List<StockItem> lowStockItems = stockDAO.getLowStockItems();

        StringBuilder sb = new StringBuilder();
        sb.append("LOW STOCK REPORT\n\n");
        sb.append(String.format("%-15s %-25s %-10s %-10s\n", "Item ID", "Description", "Avail", "Limit"));
        sb.append("---------------------------------------------------------------------\n");

        for (StockItem item : lowStockItems) {
            sb.append(String.format("%-15s %-25s %-10d %-10d\n",
                    item.getItemId(),
                    item.getDescription(),
                    item.getAvailability(),
                    item.getStockLimit()));
        }

        sb.append("\nTotal low stock items: ").append(lowStockItems.size());
        return sb.toString();
    }

    public String generateDebtorReport() {
        List<AccountHolder> holders = customerDAO.getAllAccountHolders();

        StringBuilder sb = new StringBuilder();
        sb.append("DEBTOR REPORT\n\n");
        sb.append(String.format("%-8s %-24s %-14s %-14s\n", "ID", "Customer", "Status", "Balance"));
        sb.append("---------------------------------------------------------------------\n");

        double totalDebt = 0;
        int debtorCount = 0;

        for (AccountHolder holder : holders) {
            if (holder.getOutstandingBalance() > 0) {
                sb.append(String.format("%-8d %-24s %-14s £%-14.2f\n",
                        holder.getAccountId(),
                        holder.getFullName(),
                        holder.getAccountStatus(),
                        holder.getOutstandingBalance()));

                totalDebt += holder.getOutstandingBalance();
                debtorCount++;
            }
        }

        sb.append("\nNumber of debtors: ").append(debtorCount).append("\n");
        sb.append(String.format("Total outstanding debt: £%.2f\n", totalDebt));

        return sb.toString();
    }
}