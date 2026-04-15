package API;

import java.util.List;
import java.util.Map;


 //

 //CA to PU Interface
 //Allows PU (Purchasing) to browse the CA pharmacy catalogue.
 //PU calls these methods to view available products.

public interface CA_PU_interface {

     //Returns all catalogue items.
     //Each map contains: item_id, description, package_type, unit, units_per_pack, cost_per_unit, availability

    List<Map<String, String>> getCatalogueItems();


     //Searches catalogue items by keyword (matches description).
    List<Map<String, String>> searchCatalogueItems(String keyword);

    /**
     Returns a single catalogue item by its ID.
     the item ID
     map of item details, or null if not found
     */
    Map<String, String> getCatalogueItemById(String itemId);
}
