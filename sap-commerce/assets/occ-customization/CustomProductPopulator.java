/*
 * CustomProductPopulator.java
 * Populator for converting CustomProductData to CustomProductWsDTO.
 * Used by converters in the OCC layer.
 */
package com.company.populators;

import com.company.dto.CustomProductWsDTO;
import com.company.facades.data.CustomProductData;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

/**
 * Populates CustomProductWsDTO from CustomProductData.
 *
 * Populators fill specific fields in target objects.
 * Multiple populators can be chained for different field levels.
 */
public class CustomProductPopulator implements Populator<CustomProductData, CustomProductWsDTO> {

    @Override
    public void populate(final CustomProductData source, final CustomProductWsDTO target)
            throws ConversionException {

        if (source == null) {
            throw new ConversionException("Source cannot be null");
        }

        // Basic fields
        target.setCode(source.getCode());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setSummary(source.getSummary());
        target.setUrl(source.getUrl());

        // Stock information
        target.setStockStatus(source.getStockStatus());
        target.setStockLevel(source.getStockLevel());
        target.setPurchasable(source.isPurchasable());

        // Category and brand
        target.setCategoryCode(source.getCategoryCode());
        target.setCategoryName(source.getCategoryName());
        target.setBrandName(source.getBrandName());

        // Reviews
        target.setAverageRating(source.getAverageRating());
        target.setNumberOfReviews(source.getNumberOfReviews());

        // Custom fields
        target.setCustomField(source.getCustomField());
        target.setCustomStatus(source.getCustomStatus());

        // Image URL (simple field)
        target.setImageUrl(source.getImageUrl());
    }
}
