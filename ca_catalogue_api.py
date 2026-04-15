import requests

class CACatalogueAPI:
    """
    CA Catalogue API Client
    Connects to CA's running application to fetch catalogue data.
    """

    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url

    def get_catalogue_items(self):
        """Returns all catalogue items as a list of dicts."""
        r = requests.get(f"{self.base_url}/catalogue")
        r.raise_for_status()
        return r.json()

    def search_catalogue_items(self, keyword):
        """Searches catalogue by keyword (matches description)."""
        r = requests.get(f"{self.base_url}/catalogue/search", params={"q": keyword})
        r.raise_for_status()
        return r.json()

    def get_catalogue_item_by_id(self, item_id):
        """Returns a single item by ID, or None if not found."""
        r = requests.get(f"{self.base_url}/catalogue/item", params={"id": item_id})
        r.raise_for_status()
        data = r.json()
        return data if isinstance(data, dict) else None