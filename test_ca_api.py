from ca_catalogue_api import CACatalogueAPI

api = CACatalogueAPI("http://localhost:8080")

# Get all items
items = api.get_catalogue_items()
print(f"Total items: {len(items)}\n")
for item in items:
    print(f"  {item['item_id']} - {item['description']}")
    print(f"    Â£{item['cost_per_unit']} per {item['unit']} | {item['units_per_pack']} per pack")
    print(f"    Availability: {item['availability']} packs")
    print()

# Search
results = api.search_catalogue_items("para")
print(f"Search 'para': {len(results)} results")
for r in results:
    print(f"  {r['item_id']} - {r['description']} - Availability: {r['availability']} packs")