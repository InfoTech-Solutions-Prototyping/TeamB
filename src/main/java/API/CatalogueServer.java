package API;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.valinor.iposca.dao.SACatalogueDAO;
import com.valinor.iposca.model.SACatalogueItem;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

// HTTP server that exposes CA's catalogue to PU over localhost:8080
// PU calls these endpoints from Python using ca_catalogue_api.py


public class CatalogueServer {

    private HttpServer server;

    // Starts the server
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/catalogue", this::handleGetAll);
        server.createContext("/catalogue/search", this::handleSearch);
        server.createContext("/catalogue/item", this::handleGetById);

        server.setExecutor(null);
        server.start();
        System.out.println("CA Catalogue API running on http://localhost:8080");
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    // gets catalogue items as JSON array
    private void handleGetAll(HttpExchange ex) throws IOException {
        SACatalogueDAO dao = new SACatalogueDAO();
        List<SACatalogueItem> items = dao.getAll();
        sendJson(ex, itemsToJson(items));
    }

    // returns items matching description keywords
    private void handleSearch(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        String keyword = "";
        if (query != null && query.startsWith("q=")) {
            keyword = URLDecoder.decode(query.substring(2), StandardCharsets.UTF_8);
        }
        SACatalogueDAO dao = new SACatalogueDAO();
        List<SACatalogueItem> items = keyword.isEmpty() ? dao.getAll() : dao.search(keyword);
        sendJson(ex, itemsToJson(items));
    }

    // returns a single item by ID
    private void handleGetById(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        String id = "";
        if (query != null && query.startsWith("id=")) {
            id = URLDecoder.decode(query.substring(2), StandardCharsets.UTF_8);
        }
        SACatalogueDAO dao = new SACatalogueDAO();
        SACatalogueItem item = dao.getById(id);
        if (item == null) {
            sendJson(ex, "[]");
        } else {
            sendJson(ex, itemToJson(item));
        }
    }

    private void sendJson(HttpExchange ex, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private String itemsToJson(List<SACatalogueItem> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(itemToJson(items.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String itemToJson(SACatalogueItem it) {
        return "{" +
                "\"item_id\":\"" + escape(it.getItemId()) + "\"," +
                "\"description\":\"" + escape(it.getDescription()) + "\"," +
                "\"package_type\":\"" + escape(it.getPackageType()) + "\"," +
                "\"unit\":\"" + escape(it.getUnit()) + "\"," +
                "\"units_per_pack\":" + it.getUnitsPerPack() + "," +
                "\"cost_per_unit\":" + it.getCostPerUnit() + "," +
                "\"availability\":" + it.getAvailability() +
                "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
