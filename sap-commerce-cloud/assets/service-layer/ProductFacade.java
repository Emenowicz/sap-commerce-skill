/*
 * ProductFacade.java
 * Facade interface defining the external API for product operations.
 * Facades expose only DTOs to the presentation layer, never Models.
 */
package com.example.facades;

import com.example.facades.data.ProductData;
import java.util.List;

/**
 * Facade interface for product operations.
 * Implementation naming convention: Default* prefix (e.g., DefaultProductFacade)
 */
public interface ProductFacade {

    /**
     * Retrieve product details by product code.
     * @param code the unique product code
     * @return ProductData containing product details
     * @throws UnknownIdentifierException if product not found
     */
    ProductData getProductForCode(String code);

    /**
     * Search products by text query with pagination.
     * @param query the search query string
     * @param pageSize maximum number of results per page
     * @param currentPage zero-based page index
     * @return list of matching ProductData objects
     */
    List<ProductData> searchProducts(String query, int pageSize, int currentPage);

    /**
     * Retrieve all products in a specific category.
     * @param categoryCode the category code
     * @return list of ProductData in the category
     */
    List<ProductData> getProductsForCategory(String categoryCode);

    /**
     * Update product stock level.
     * @param productCode the product code
     * @param quantity new stock quantity
     */
    void updateProductStock(String productCode, int quantity);

    /**
     * Check if a product is available (in stock).
     * @param productCode the product code
     * @return true if stock level > 0
     */
    boolean isProductAvailable(String productCode);
}
