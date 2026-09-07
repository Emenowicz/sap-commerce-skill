package com.example.facades.populators;

import com.example.facades.data.ProductData;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

/** Maps the fields owned by this focused service-layer example. */
public class ProductPopulator implements Populator<ProductModel, ProductData> {

    @Override
    public void populate(final ProductModel source, final ProductData target)
            throws ConversionException {
        if (source == null || target == null) {
            throw new ConversionException("Source and target are required");
        }
        target.setCode(source.getCode());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setSummary(source.getSummary());
    }
}
