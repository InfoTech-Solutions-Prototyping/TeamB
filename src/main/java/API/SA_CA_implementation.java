package API;

import java.sql.*;
import java.util.*;


public class SA_CA_implementation implements SA_CA_interface {

    // Database connection settings
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/ipos_sa";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "1313Ipos!";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    @Override
    public Map<String, String> authenticateMerchant(String username, String password)
            throws Exception {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password must not be blank.");
        }

        String sql = "SELECT m.merchant_id, m.username, m.account_name, m.account_status " +
                "FROM merchants m " +
                "WHERE m.username = ? AND m.password = ?";


        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Map<String, String> result = new LinkedHashMap<>();
                    result.put("username",      rs.getString("username"));
                    result.put("fullName",      rs.getString("account_name"));
                    result.put("merchantId",    rs.getString("merchant_id"));
                    result.put("accountStatus", rs.getString("account_status"));
                    return result;
                }
            }
        return null;
    }


    @Override
    public List<Map<String, String>> getCatalogueItems() throws Exception {
        String sql = "SELECT item_id, description, package_type, unit, units_in_pack, " +
                "unit_cost, availability " +
                "FROM catalogue_items WHERE availability > 0 ORDER BY item_id";
        return executeCatalogueQuery(sql, null);
    }

    @Override
    public List<Map<String, String>> searchCatalogueItems(String keyword) throws Exception {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword must not be blank.");
        }
        String sql = "SELECT item_id, description, package_type, unit, units_in_pack, " +
                "unit_cost, availability " +
                "FROM catalogue_items WHERE availability > 0 " +
                "AND (LOWER(item_id) LIKE ? OR LOWER(description) LIKE ?) ORDER BY item_id";
        return executeCatalogueQuery(sql, "%" + keyword.toLowerCase() + "%");
    }

    private List<Map<String, String>> executeCatalogueQuery(String sql, String param)
            throws Exception {
        List<Map<String, String>> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) { ps.setString(1, param); ps.setString(2, param); }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("itemId",       rs.getString("item_id"));
                item.put("description",  rs.getString("description"));
                item.put("packageType",  rs.getString("package_type"));
                item.put("unit",         rs.getString("unit"));
                item.put("unitsPerPack", String.valueOf(rs.getInt("units_in_pack")));
                item.put("costPerUnit",  String.format("%.2f", rs.getDouble("unit_cost")));
                item.put("availability", String.valueOf(rs.getInt("availability")));
                results.add(item);
            }
        }
        return results;
    }


    @Override
    public String placeOrder(String merchantId, List<Map<String, String>> orderLines)
            throws Exception {

        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("merchantId must not be blank.");
        }
        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException("orderLines must contain at least one item.");
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Load merchant
                PreparedStatement mSt = conn.prepareStatement(
                        "SELECT account_status, current_balance, credit_limit " +
                                "FROM merchants WHERE merchant_id = ?");
                mSt.setString(1, merchantId);
                ResultSet mRs = mSt.executeQuery();

                if (!mRs.next()) {
                    conn.rollback();
                    throw new IllegalArgumentException("No merchant found with ID: " + merchantId);
                }

                String status      = mRs.getString("account_status");
                double balance     = mRs.getDouble("current_balance");
                double creditLimit = mRs.getDouble("credit_limit");

                if (!"normal".equals(status)) { conn.rollback(); return null; }

                // 2. Validate lines and compute total
                double orderTotal = 0;
                List<Object[]> resolvedLines = new ArrayList<>();

                for (Map<String, String> line : orderLines) {
                    String itemId = line.get("itemId");
                    String qtyStr = line.get("quantity");

                    if (itemId == null || itemId.isBlank() || qtyStr == null || qtyStr.isBlank()) {
                        conn.rollback();
                        throw new IllegalArgumentException("Each line must have itemId and quantity.");
                    }

                    int qty;
                    try { qty = Integer.parseInt(qtyStr); }
                    catch (NumberFormatException e) {
                        conn.rollback();
                        throw new IllegalArgumentException("Quantity must be a whole number: " + qtyStr);
                    }
                    if (qty <= 0) {
                        conn.rollback();
                        throw new IllegalArgumentException("Quantity must be positive.");
                    }

                    PreparedStatement iSt = conn.prepareStatement(
                            "SELECT description, unit_cost, availability " +
                                    "FROM catalogue_items WHERE item_id = ?");
                    iSt.setString(1, itemId);
                    ResultSet iRs = iSt.executeQuery();

                    if (!iRs.next()) {
                        conn.rollback();
                        throw new IllegalArgumentException("Item not found: " + itemId);
                    }

                    String desc  = iRs.getString("description");
                    double unit  = iRs.getDouble("unit_cost");
                    int avail    = iRs.getInt("availability");

                    if (qty > avail) {
                        conn.rollback();
                        throw new IllegalArgumentException(
                                "Insufficient stock for " + itemId + ". Available: " + avail);
                    }

                    double lineTotal = unit * qty;
                    orderTotal += lineTotal;
                    resolvedLines.add(new Object[]{itemId, desc, qty, unit, lineTotal});
                }

                // 3. Discount
                double discount = calculateDiscount(conn, merchantId, orderTotal);
                double net      = orderTotal - discount;

                // 4. Credit limit check
                if (balance + net > creditLimit) { conn.rollback(); return null; }

                // 5. Insert order
                PreparedStatement oSt = conn.prepareStatement(
                        "INSERT INTO orders (merchant_id, order_date, order_status, " +
                                "total_amount, discount_amount, net_amount) " +
                                "VALUES (?, NOW(), 'accepted', ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                oSt.setString(1, merchantId);
                oSt.setDouble(2, orderTotal);
                oSt.setDouble(3, discount);
                oSt.setDouble(4, net);
                oSt.executeUpdate();
                ResultSet keys = oSt.getGeneratedKeys(); keys.next();
                int orderId = keys.getInt(1);

                // 6. Insert order_items + reduce stock
                PreparedStatement liSt = conn.prepareStatement(
                        "INSERT INTO order_items (order_id, item_id, description, quantity, " +
                                "unit_cost, line_total) VALUES (?, ?, ?, ?, ?, ?)");
                PreparedStatement sSt = conn.prepareStatement(
                        "UPDATE catalogue_items SET availability = availability - ? WHERE item_id = ?");

                for (Object[] line : resolvedLines) {
                    liSt.setInt(1, orderId);
                    liSt.setString(2, (String) line[0]);
                    liSt.setString(3, (String) line[1]);
                    liSt.setInt(4,    (int)    line[2]);
                    liSt.setDouble(5, (double) line[3]);
                    liSt.setDouble(6, (double) line[4]);
                    liSt.executeUpdate();

                    sSt.setInt(1,    (int)    line[2]);
                    sSt.setString(2, (String) line[0]);
                    sSt.executeUpdate();
                }

                // 7. Update merchant balance
                PreparedStatement bSt = conn.prepareStatement(
                        "UPDATE merchants SET current_balance = current_balance + ? WHERE merchant_id = ?");
                bSt.setDouble(1, net); bSt.setString(2, merchantId);
                bSt.executeUpdate();

                conn.commit();
                return String.valueOf(orderId);

            } catch (Exception ex) { conn.rollback(); throw ex; }
        }
    }

    private double calculateDiscount(Connection conn, String merchantId, double total)
            throws Exception {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT plan_type, fixed_rate FROM discount_plans " +
                        "WHERE merchant_id = ? AND is_active = TRUE");
        ps.setString(1, merchantId);
        ResultSet rs = ps.executeQuery();
        if (rs.next() && "fixed".equalsIgnoreCase(rs.getString("plan_type"))) {
            return total * (rs.getDouble("fixed_rate") / 100.0);
        }
        return 0.00;
    }


    @Override
    public List<Map<String, String>> getOrdersByMerchant(String merchantId) throws Exception {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("merchantId must not be blank.");
        }
        String sql = "SELECT order_id, merchant_id, order_date, order_status, " +
                "total_amount, discount_amount, net_amount, courier, dispatched_at, delivered_at " +
                "FROM orders WHERE merchant_id = ? ORDER BY order_date DESC";

        List<Map<String, String>> results = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, merchantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) results.add(buildOrderMap(rs));
        }
        return results;
    }

    @Override
    public Map<String, String> getOrderDetails(String orderId) throws Exception {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank.");
        }
        String sql = "SELECT order_id, merchant_id, order_date, order_status, " +
                "total_amount, discount_amount, net_amount, courier, dispatched_at, delivered_at " +
                "FROM orders WHERE order_id = ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(orderId));
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            Map<String, String> order = buildOrderMap(rs);

            PreparedStatement liPs = conn.prepareStatement(
                    "SELECT item_id, description, quantity, unit_cost, line_total " +
                            "FROM order_items WHERE order_id = ?");
            liPs.setInt(1, Integer.parseInt(orderId));
            ResultSet liRs = liPs.executeQuery();
            StringBuilder items = new StringBuilder();
            while (liRs.next()) {
                if (items.length() > 0) items.append("; ");
                items.append(liRs.getString("item_id"))
                        .append("|").append(liRs.getString("description"))
                        .append("|").append(liRs.getInt("quantity"))
                        .append("|").append(String.format("%.2f", liRs.getDouble("unit_cost")))
                        .append("|").append(String.format("%.2f", liRs.getDouble("line_total")));
            }
            order.put("items", items.toString());
            return order;
        }
    }

    private Map<String, String> buildOrderMap(ResultSet rs) throws SQLException {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("orderId",        String.valueOf(rs.getInt("order_id")));
        m.put("merchantId",     rs.getString("merchant_id"));
        m.put("orderDate",      rs.getTimestamp("order_date") != null ? rs.getTimestamp("order_date").toString() : "");
        m.put("orderStatus",    rs.getString("order_status"));
        m.put("totalAmount",    String.format("%.2f", rs.getDouble("total_amount")));
        m.put("discountAmount", String.format("%.2f", rs.getDouble("discount_amount")));
        m.put("netAmount",      String.format("%.2f", rs.getDouble("net_amount")));
        m.put("courier",        rs.getString("courier") != null ? rs.getString("courier") : "");
        m.put("dispatchedAt",   rs.getTimestamp("dispatched_at") != null ? rs.getTimestamp("dispatched_at").toString() : "");
        m.put("deliveredAt",    rs.getTimestamp("delivered_at") != null ? rs.getTimestamp("delivered_at").toString() : "");
        return m;
    }



    @Override
    public List<Map<String, String>> getInvoicesByMerchant(String merchantId) throws Exception {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("merchantId must not be blank.");
        }
        String sql = "SELECT invoice_id, order_id, merchant_id, invoice_date, " +
                "total_amount, discount, net_amount, is_paid " +
                "FROM invoices WHERE merchant_id = ? ORDER BY invoice_date DESC";

        List<Map<String, String>> results = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, merchantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) results.add(buildInvoiceMap(rs));
        }
        return results;
    }

    @Override
    public Map<String, String> getInvoiceDetails(String invoiceId) throws Exception {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new IllegalArgumentException("invoiceId must not be blank.");
        }
        String sql = "SELECT invoice_id, order_id, merchant_id, invoice_date, " +
                "total_amount, discount, net_amount, is_paid " +
                "FROM invoices WHERE invoice_id = ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(invoiceId));
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return buildInvoiceMap(rs);
        }
    }

    private Map<String, String> buildInvoiceMap(ResultSet rs) throws SQLException {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("invoiceId",   String.valueOf(rs.getInt("invoice_id")));
        m.put("orderId",     String.valueOf(rs.getInt("order_id")));
        m.put("merchantId",  rs.getString("merchant_id"));
        m.put("invoiceDate", rs.getTimestamp("invoice_date") != null ? rs.getTimestamp("invoice_date").toString() : "");
        m.put("totalAmount", String.format("%.2f", rs.getDouble("total_amount")));
        m.put("discount",    String.format("%.2f", rs.getDouble("discount")));
        m.put("netAmount",   String.format("%.2f", rs.getDouble("net_amount")));
        m.put("isPaid",      rs.getBoolean("is_paid") ? "true" : "false");
        return m;
    }



    @Override
    public Map<String, String> getMerchantBalanceAndStatus(String merchantId) throws Exception {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("merchantId must not be blank.");
        }
        String sql = "SELECT merchant_id, account_name, current_balance, credit_limit, account_status " +
                "FROM merchants WHERE merchant_id = ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, merchantId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            String status  = rs.getString("account_status");
            double balance = rs.getDouble("current_balance");
            boolean showReminder = balance > 0 && !"normal".equals(status);

            Map<String, String> m = new LinkedHashMap<>();
            m.put("merchantId",         rs.getString("merchant_id"));
            m.put("accountName",        rs.getString("account_name"));
            m.put("currentBalance",     String.format("%.2f", balance));
            m.put("creditLimit",        String.format("%.2f", rs.getDouble("credit_limit")));
            m.put("accountStatus",      status);
            m.put("showDebtorReminder", String.valueOf(showReminder));
            return m;
        }
    }
}