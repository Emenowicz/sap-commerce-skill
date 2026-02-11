/*
 * DefaultProductFacade.java
 * Default implementation of ProductFacade.
 * Follows SAP Commerce naming convention: Default* prefix for implementations.
 */
package com.company.facades.impl;

import com.company.facades.ProductFacade;
import com.company.facades.data.ProductData;
import com.company.core.services.ProductService;

import de.hybris.platform.converters.Converters;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;

import org.springframework.util.Assert;
import java.util.List;

/**
 * Default implementation of ProductFacade.
 *
 * Pattern: Facade acts as thin layer that:
 * 1. Delegates business logic to services
 * 2. Converts Models to DTOs using converters
 * 3. Handles null checks and exception transformation
 */
public class DefaultProductFacade implements ProductFacade {

    private ProductService productService;
    private Converter<ProductModel, ProductData> productConverter;

    @Override
    public ProductData getProductForCode(final String code) {
        Assert.notNull(code, "Product code cannot be null");

        // Delegate to service layer
        final ProductModel productModel = productService.getProductForCode(code);

        if (productModel == null) {
            throw new UnknownIdentifierException("Product not found for code: " + code);
        }

        // Convert Model to DTO - never expose Models to presentation layer
        return productConverter.convert(productModel);
    }

    @Override
    public List<ProductData> searchProducts(final String query, final int pageSize, final int currentPage) {
        Assert.notNull(query, "Search query cannot be null");
        Assert.isTrue(pageSize > 0, "Page size must be positive");

        final List<ProductModel> products = productService.searchProducts(query, pageSize, currentPage);

        // Batch convert all models to DTOs
        return Converters.convertAll(products, productConverter);
    }

    @Override
    public List<ProductData> getProductsForCategory(final String categoryCode) {
        Assert.notNull(categoryCode, "Category code cannot be null");

        final List<ProductModel> products = productService.getProductsForCategory(categoryCode);
        return Converters.convertAll(products, productConverter);
    }

    @Override
    public void updateProductStock(final String productCode, final int quantity) {
        Assert.notNull(productCode, "Product code cannot be null");

        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative: " + quantity);
        }

        productService.updateProductStock(productCode, quantity);
    }

    @Override
    public boolean isProductAvailable(final String productCode) {
        Assert.notNull(productCode, "Product code cannot be null");
        return productService.isProductInStock(productCode);
    }

    // Setter injection for Spring configuration
    public void setProductService(final ProductService productService) {
        this.productService = productService;
    }

    public void setProductConverter(final Converter<ProductModel, ProductData> productConverter) {
        this.productConverter = productConverter;
    }
}
