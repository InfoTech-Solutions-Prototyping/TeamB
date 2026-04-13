package API;

import java.util.List;
import java.util.Map;


public interface SA_CA_interface {

    Map<String, String> authenticateMerchant(String username, String password) throws Exception;

    List<Map<String, String>> getCatalogueItems() throws Exception;

    List<Map<String, String>> searchCatalogueItems(String keyword) throws Exception;

    String placeOrder(String merchantId, List<Map<String, String>> orderLines) throws Exception;

    List<Map<String, String>> getOrdersByMerchant(String merchantId) throws Exception;

    Map<String, String> getOrderDetails(String orderId) throws Exception;


    List<Map<String, String>> getInvoicesByMerchant(String merchantId) throws Exception;

    Map<String, String> getInvoiceDetails(String invoiceId) throws Exception;


    Map<String, String> getMerchantBalanceAndStatus(String merchantId) throws Exception;
}
