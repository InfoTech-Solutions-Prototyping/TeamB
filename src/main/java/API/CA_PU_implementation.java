package API;

import java.sql.*;
import java.util.*;


 //CA -> PU Implementation
 //Reads from CA's local SQLite catalogue cache (ipos_ca.db).
 //PU does not need MySQL — this reads from SQLite only.

public class CA_PU_implementation implements CA_PU_interface {

    private final String dbPath;


     //path to ipos_ca.db
     //e.g. "/Users/towhid/Documents/Valinor-IPOS-CA/ipos_ca.db"

    public CA_PU_implementation(String dbPath) {
        this.dbPath = dbPath;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    @Override
    public List<Map<String, String>> getCatalogueItems() {
        List<Map<String, String>> items = new ArrayList<>();
        String sql = "SELECT item_id, description, package_type, unit, "
                + "units_per_pack, cost_per_unit, availability FROM sa_catalogue";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                items.add(rowToMap(rs));
            }
        } catch (SQLException e) {
            System.err.println("CA_PU getCatalogueItems error: " + e.getMessage());
        }
        return items;
    }

    @Override
    public List<Map<String, String>> searchCatalogueItems(String keyword) {
        List<Map<String, String>> items = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return getCatalogueItems();
        }

        String sql = "SELECT item_id, description, package_type, unit, "
                + "units_per_pack, cost_per_unit, availability FROM sa_catalogue "
                + "WHERE description LIKE ?";

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("CA_PU searchCatalogueItems error: " + e.getMessage());
        }
        return items;
    }

    @Override
    public Map<String, String> getCatalogueItemById(String itemId) {
        String sql = "SELECT item_id, description, package_type, unit, "
                + "units_per_pack, cost_per_unit, availability FROM sa_catalogue "
                + "WHERE item_id = ?";

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("CA_PU getCatalogueItemById error: " + e.getMessage());
        }
        return null;
    }

    private Map<String, String> rowToMap(ResultSet rs) throws SQLException {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("item_id",       rs.getString("item_id"));
        map.put("description",   rs.getString("description"));
        map.put("package_type",  rs.getString("package_type"));
        map.put("unit",          rs.getString("unit"));
        map.put("units_per_pack", rs.getString("units_per_pack"));
        map.put("cost_per_unit", rs.getString("cost_per_unit"));
        map.put("availability",  rs.getString("availability"));
        return map;
    }
}
