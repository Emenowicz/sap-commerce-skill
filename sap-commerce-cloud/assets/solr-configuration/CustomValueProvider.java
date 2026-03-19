package com.example.search.providers;

import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.solrfacetsearch.config.IndexConfig;
import de.hybris.platform.solrfacetsearch.config.IndexedProperty;
import de.hybris.platform.solrfacetsearch.config.exceptions.FieldValueProviderException;
import de.hybris.platform.solrfacetsearch.provider.FieldValue;
import de.hybris.platform.solrfacetsearch.provider.FieldValueProvider;
import de.hybris.platform.solrfacetsearch.provider.impl.AbstractPropertyFieldValueProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Custom Solr value provider template.
 *
 * Extracts a computed value from the product model for indexing.
 * Register as a Spring bean with parent="abstractPropertyFieldValueProvider"
 * and reference in SolrIndexedProperty ImpEx via fieldValueProvider.
 *
 * For localized fields, use {@link de.hybris.platform.solrfacetsearch.provider.impl.AbstractLocalizedValueResolver}
 * or check the locale in the IndexConfig.
 */
public class CustomValueProvider extends AbstractPropertyFieldValueProvider implements FieldValueProvider {

    @Override
    public Collection<FieldValue> getFieldValues(
            final IndexConfig indexConfig,
            final IndexedProperty indexedProperty,
            final Object model) throws FieldValueProviderException {

        if (!(model instanceof ProductModel)) {
            throw new FieldValueProviderException("Expected ProductModel but got: " + model.getClass().getName());
        }

        final ProductModel product = (ProductModel) model;
        final List<FieldValue> fieldValues = new ArrayList<>();

        // TODO: Replace with actual value computation logic
        // Examples:
        //   - Combine multiple attributes into a single search field
        //   - Compute a score or rating from related models
        //   - Extract values from custom attributes or relations
        final Object value = computeFieldValue(product);

        if (value != null) {
            addFieldValues(fieldValues, indexedProperty, value);
        }

        return fieldValues;
    }

    /**
     * Compute the value to be indexed for the given product.
     *
     * @param product the product model
     * @return the computed value, or null if nothing to index
     */
    private Object computeFieldValue(final ProductModel product) {
        // Example: return a custom attribute
        // return product.getProperty("customAttribute");

        // Example: return a computed string
        // return product.getCode() + " - " + product.getName();

        return null;
    }
}
