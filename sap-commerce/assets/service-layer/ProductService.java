/*
 * ProductService.java
 * Service interface defining business operations for products.
 * Services work with Models and contain business logic.
 */
package com.company.core.services;

import de.hybris.platform.core.model.product.ProductModel;
import java.util.List;

/**
 * Service interface for product business operations.
 * Implementation naming convention: Default* prefix (e.g., DefaultProductService)
 */
public interface ProductService {

    /**
     * Retrieve a product by its unique code.
     * @param code the product code
     * @return ProductModel or null if not found
     */
    ProductModel getProductForCode(String code);

    /**
     * Search products by text query with pagination.
     * @param query search text
     * @param pageSize results per page
     * @param currentPage zero-based page index
     * @return list of matching ProductModel objects
     */
    List<ProductModel> searchProducts(String query, int pageSize, int currentPage);

    /**
     * Get all products belonging to a category.
     * @param categoryCode the category code
     * @return list of ProductModel in the category
     */
    List<ProductModel> getProductsForCategory(String categoryCode);

    /**
     * Update the stock level for a product. This method is transactional.
     * @param productCode the product code
     * @param quantity new stock quantity (must be non-negative)
     */
    void updateProductStock(String productCode, int quantity);

    /**
     * Check if product has available stock.
     * @param productCode the product code
     * @return true if stock level > 0
     */
    boolean isProductInStock(String productCode);

    /**
     * Create a new product with the given attributes.
     * @param code unique product code
     * @param name product name
     * @param catalogVersion the catalog version
     * @return the created ProductModel
     */
    ProductModel createProduct(String code, String name, String catalogVersion);

    /**
     * Save changes to a product model.
     * @param product the product to save
     */
    void saveProduct(ProductModel product);
}
