/*
 * DefaultProductDAO.java
 * Default implementation of ProductDAO using FlexibleSearch.
 * Demonstrates query patterns, pagination, and joins.
 */
package com.example.core.daos.impl;

import com.example.core.daos.ProductDAO;

import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.Collections;
import java.util.List;

/**
 * Default implementation of ProductDAO using FlexibleSearch.
 *
 * <p><strong>Important:</strong> DAOs must NOT be annotated with {@code @Transactional}.
 * Transaction boundaries are managed at the Service layer. Placing {@code @Transactional}
 * on DAO methods prevents services from coordinating multiple DAO calls within a single
 * atomic transaction.</p>
 *
 * FlexibleSearch patterns demonstrated:
 * - Basic SELECT with WHERE clause
 * - Parameterized queries (prevents SQL injection)
 * - JOIN queries for related entities
 * - Pagination with count, start, and limit
 * - LIKE queries for text search
 */
public class DefaultProductDAO implements ProductDAO {

    private FlexibleSearchService flexibleSearchService;

    @Override
    public ProductModel findByCode(final String code) {
        // Basic query: SELECT pk WHERE code matches
        final String queryString = "SELECT {pk} FROM {Product} WHERE {code} = ?code";

        final FlexibleSearchQuery query = new FlexibleSearchQuery(queryString);
        query.addQueryParameter("code", code);
        query.setResultClassList(Collections.singletonList(ProductModel.class));

        final SearchResult<ProductModel> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }

    @Override
    public ProductModel findByCodeAndCatalogVersion(final String code, final String catalogId,
            final String catalogVersionName) {
        // JOIN query: Product joined with CatalogVersion
        final String queryString =
            "SELECT {p.pk} FROM {Product AS p " +
            "JOIN CatalogVersion AS cv ON {p.catalogVersion} = {cv.pk} " +
            "JOIN Catalog AS c ON {cv.catalog} = {c.pk}} " +
            "WHERE {p.code} = ?code " +
            "AND {c.id} = ?catalogId " +
            "AND {cv.version} = ?versionName";

        final FlexibleSearchQuery query = new FlexibleSearchQuery(queryString);
        query.addQueryParameter("code", code);
        query.addQueryParameter("catalogId", catalogId);
        query.addQueryParameter("versionName", catalogVersionName);

        final SearchResult<ProductModel> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }

    @Override
    public List<ProductModel> searchByText(final String searchText, final int limit, final int offset) {
        // LIKE query with case-insensitive search
        final String queryString =
            "SELECT {pk} FROM {Product} " +
            "WHERE LOWER({name}) LIKE LOWER(?searchText) " +
            "ORDER BY {name} ASC";

        final FlexibleSearchQuery query = new FlexibleSearchQuery(queryString);
        query.addQueryParameter("searchText", "%" + searchText + "%");

        // Pagination: setStart = offset, setCount = limit
        query.setStart(offset);
        query.setCount(limit);

        final SearchResult<ProductModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<ProductModel> findByCategory(final String categoryCode) {
        // Many-to-many relation query via supercategories
        final String queryString =
            "SELECT {p.pk} FROM {Product AS p " +
            "JOIN CategoryProductRelation AS rel ON {p.pk} = {rel.target} " +
            "JOIN Category AS c ON {rel.source} = {c.pk}} " +
            "WHERE {c.code} = ?categoryCode";

        final FlexibleSearchQuery query = new FlexibleSearchQuery(queryString);
        query.addQueryParameter("categoryCode", categoryCode);

        final SearchResult<ProductModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<ProductModel> findLowStockProducts(final int threshold) {
        // Join with StockLevel
        final String queryString =
            "SELECT {p.pk} FROM {Product AS p " +
            "JOIN StockLevel AS sl ON {p.pk} = {sl.product}} " +
            "WHERE {sl.available} < ?threshold AND {sl.available} > 0";

        final FlexibleSearchQuery query = new FlexibleSearchQuery(queryString);
        query.addQueryParameter("threshold", Integer.valueOf(threshold));

        final SearchResult<ProductModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public int countAllProducts() {
        // COUNT query
        final String queryString = "SELECT COUNT({pk}) FROM {Product}";

        final FlexibleSearchQuery query = new FlexibleSearchQuery(queryString);
        query.setResultClassList(Collections.singletonList(Integer.class));

        final SearchResult<Integer> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? 0 : result.getResult().get(0);
    }

    // Setter injection
    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
}
