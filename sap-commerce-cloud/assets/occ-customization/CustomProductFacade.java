package com.example.facades;

import com.example.facades.data.CustomProductData;

import java.util.List;

/**
 * Business-facing contract consumed by the custom OCC controller.
 * Implement this interface in the facade extension and expose it as the
 * {@code customProductFacade} Spring bean.
 */
public interface CustomProductFacade {

    List<CustomProductData> searchProducts(String query, int currentPage, int pageSize);

    long getTotalCount(String query);

    CustomProductData getProductForCode(String productCode);

    CustomProductData createProduct(CustomProductData product);

    CustomProductData updateProduct(CustomProductData product);

    void deleteProduct(String productCode);
}
