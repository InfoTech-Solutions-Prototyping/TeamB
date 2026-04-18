package API;

import java.util.List;
import java.util.Map;

// This is api was not used in the end, as we went for the HTTP approach instead
// See API/CatalogueServer.java for the live implementation


//CA to PU Interface
//Allows PU (Purchasing) to browse the CA pharmacy catalogue.
//PU calls these methods to view available products.

public interface CA_PU_interface {

    //Returns all catalogue items
    List<Map<String, String>> getCatalogueItems();

    //Searches catalogue items by keyword (matches description)
    List<Map<String, String>> searchCatalogueItems(String keyword);

    //Returns a single catalogue item by its ID.

    Map<String, String> getCatalogueItemById(String itemId);
}
