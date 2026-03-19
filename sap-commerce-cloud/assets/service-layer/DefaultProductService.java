/*
 * DefaultProductService.java
 * Default implementation of ProductService.
 * Contains business logic, validation, and transaction management.
 */
package com.example.core.services.impl;

import com.example.core.daos.ProductDAO;
import com.example.core.services.ProductService;

import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.ordersplitting.model.StockLevelModel;
import de.hybris.platform.ordersplitting.model.WarehouseModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.stock.StockService;
import de.hybris.platform.store.services.WarehouseService;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of ProductService.
 *
 * Pattern: Service layer responsibilities:
 * 1. Business logic and domain rules
 * 2. Input validation
 * 3. Transaction management (via @Transactional)
 * 4. Coordination of DAO operations
 * 5. Model creation and persistence via ModelService
 */
public class DefaultProductService implements ProductService {

    private ProductDAO productDAO;
    private ModelService modelService;
    private WarehouseService warehouseService;
    private StockService stockService;

    @Override
    public ProductModel getProductForCode(final String code) {
        Assert.notNull(code, "Product code cannot be null");
        return productDAO.findByCode(code);
    }

    @Override
    public List<ProductModel> searchProducts(final String query, final int pageSize, final int currentPage) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        final int offset = currentPage * pageSize;
        return productDAO.searchByText(query, pageSize, offset);
    }

    @Override
    public List<ProductModel> getProductsForCategory(final String categoryCode) {
        Assert.notNull(categoryCode, "Category code cannot be null");
        return productDAO.findByCategory(categoryCode);
    }

    @Override
    @Transactional
    // @Transactional ensures atomicity - place on service methods, not DAOs
    public void updateProductStock(final String productCode, final int quantity) {
        Assert.notNull(productCode, "Product code cannot be null");

        // Business validation
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative: " + quantity);
        }

        final ProductModel product = productDAO.findByCode(productCode);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productCode);
        }

        // Update stock via StockLevelModel + WarehouseModel (correct SAP Commerce pattern)
        final Collection<WarehouseModel> warehouses = warehouseService.getWarehouses();
        final WarehouseModel warehouse = warehouses.iterator().next();

        final StockLevelModel stockLevel = modelService.create(StockLevelModel.class);
        stockLevel.setProductCode(product.getCode());
        stockLevel.setWarehouse(warehouse);
        stockLevel.setAvailable(quantity);
        modelService.save(stockLevel);
    }

    @Override
    public boolean isProductInStock(final String productCode) {
        Assert.notNull(productCode, "Product code cannot be null");

        final ProductModel product = productDAO.findByCode(productCode);
        if (product == null) {
            return false;
        }

        // Check stock via StockService — getTotalStockLevelAmount sums across all warehouses
        final Long totalStock = stockService.getTotalStockLevelAmount(product);
        return totalStock != null && totalStock > 0;
    }

    @Override
    @Transactional
    public ProductModel createProduct(final String code, final String name, final String catalogVersionId) {
        Assert.notNull(code, "Product code cannot be null");
        Assert.notNull(name, "Product name cannot be null");

        // Check if product already exists
        final ProductModel existing = productDAO.findByCode(code);
        if (existing != null) {
            throw new IllegalArgumentException("Product already exists with code: " + code);
        }

        // Create via ModelService
        final ProductModel product = modelService.create(ProductModel.class);
        product.setCode(code);
        product.setName(name);

        modelService.save(product);
        return product;
    }

    @Override
    @Transactional
    public void saveProduct(final ProductModel product) {
        Assert.notNull(product, "Product cannot be null");
        validateProduct(product);
        modelService.save(product);
    }

    private void validateProduct(final ProductModel product) {
        if (product.getCode() == null || product.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Product code is required");
        }
    }

    // Setter injection
    public void setProductDAO(final ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setWarehouseService(final WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    public void setStockService(final StockService stockService) {
        this.stockService = stockService;
    }
}
