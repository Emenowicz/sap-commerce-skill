/*
 * DefaultProductService.java
 * Default implementation of ProductService.
 * Contains business logic, validation, and transaction management.
 */
package com.example.core.services.impl;

import com.example.core.daos.ProductDAO;
import com.example.core.services.ProductService;

import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.ordersplitting.model.StockLevelModel;
import de.hybris.platform.ordersplitting.model.WarehouseModel;
import de.hybris.platform.ordersplitting.WarehouseService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.stock.StockService;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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
    private de.hybris.platform.product.ProductService platformProductService;
    private ModelService modelService;
    private WarehouseService warehouseService;
    private StockService stockService;

    @Override
    public ProductModel getProductForCode(final String code) {
        Assert.notNull(code, "Product code cannot be null");
        return platformProductService.getProductForCode(code);
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
    public void updateProductStock(final String productCode, final String warehouseCode, final int quantity) {
        Assert.notNull(productCode, "Product code cannot be null");
        Assert.notNull(warehouseCode, "Warehouse code cannot be null");

        // Business validation
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative: " + quantity);
        }

        final ProductModel product = platformProductService.getProductForCode(productCode);

        final WarehouseModel warehouse = warehouseService.getWarehouseForCode(warehouseCode);
        final StockLevelModel stockLevel = stockService.getStockLevel(product, warehouse);
        if (stockLevel == null) {
            final StockLevelModel newStockLevel = modelService.create(StockLevelModel.class);
            newStockLevel.setProductCode(product.getCode());
            newStockLevel.setWarehouse(warehouse);
            newStockLevel.setAvailable(quantity);
            modelService.save(newStockLevel);
        } else {
            stockService.updateActualStockLevel(product, warehouse, quantity, "Product stock update");
        }
    }

    @Override
    public boolean isProductInStock(final String productCode) {
        Assert.notNull(productCode, "Product code cannot be null");

        final ProductModel product = platformProductService.getProductForCode(productCode);

        // Check stock via StockService — getTotalStockLevelAmount sums across all warehouses
        return !stockService.getAllStockLevels(product).isEmpty()
            && stockService.getTotalStockLevelAmount(product) > 0;
    }

    @Override
    @Transactional
    public ProductModel createProduct(final String code, final String name,
            final CatalogVersionModel catalogVersion) {
        Assert.notNull(code, "Product code cannot be null");
        Assert.notNull(name, "Product name cannot be null");
        Assert.notNull(catalogVersion, "Catalog version cannot be null");

        // Check if product already exists
        final ProductModel existing = productDAO.findByCodeAndCatalogVersion(code, catalogVersion);
        if (existing != null) {
            throw new IllegalArgumentException("Product already exists with code: " + code);
        }

        // Create via ModelService
        final ProductModel product = modelService.create(ProductModel.class);
        product.setCode(code);
        product.setName(name);
        product.setCatalogVersion(catalogVersion);

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

    public void setPlatformProductService(
            final de.hybris.platform.product.ProductService platformProductService) {
        this.platformProductService = platformProductService;
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
