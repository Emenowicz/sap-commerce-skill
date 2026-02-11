/*
 * ProductDAO.java
 * DAO interface for product data access operations.
 * DAOs abstract all database interactions using FlexibleSearch.
 */
package com.company.core.daos;

import de.hybris.platform.core.model.product.ProductModel;
import java.util.List;

/**
 * Data Access Object interface for Product operations.
 *
 * DAO methods should:
 * - Not contain business logic (that belongs in services)
 * - Return null or empty collections, not throw exceptions for "not found"
 * - Use parameterized queries to prevent SQL injection
 */
public interface ProductDAO {

    /**
     * Find a product by its unique code.
     * @param code the product code
     * @return ProductModel or null if not found
     */
    ProductModel findByCode(String code);

    /**
     * Find a product by code within a specific catalog version.
     * @param code the product code
     * @param catalogId the catalog ID
     * @param catalogVersionName the catalog version name
     * @return ProductModel or null if not found
     */
    ProductModel findByCodeAndCatalogVersion(String code, String catalogId, String catalogVersionName);

    /**
     * Search products by text in name or description.
     * @param searchText the text to search for
     * @param limit maximum results to return
     * @param offset number of results to skip (for pagination)
     * @return list of matching ProductModel objects
     */
    List<ProductModel> searchByText(String searchText, int limit, int offset);

    /**
     * Find all products in a category.
     * @param categoryCode the category code
     * @return list of ProductModel in the category
     */
    List<ProductModel> findByCategory(String categoryCode);

    /**
     * Find products with stock below threshold.
     * @param threshold the minimum stock level
     * @return list of ProductModel with low stock
     */
    List<ProductModel> findLowStockProducts(int threshold);

    /**
     * Count total products in the system.
     * @return total product count
     */
    int countAllProducts();
}
