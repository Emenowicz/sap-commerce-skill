/*
 * CustomProductWsDTO.java
 * Web Service DTO for custom product API responses.
 * Uses OpenAPI 3 annotations for API documentation.
 */
package com.example.dto;

import de.hybris.platform.commercewebservicescommons.dto.product.PriceWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.ImageWsDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.List;

/**
 * WsDTO for custom product representation in OCC API.
 *
 * Field levels:
 * - BASIC: code, name
 * - DEFAULT: code, name, description, price
 * - FULL: all fields
 */
@Schema(name = "CustomProduct", description = "Custom product representation")
public class CustomProductWsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Unique product code", requiredMode = Schema.RequiredMode.REQUIRED, example = "CUSTOM001")
    private String code;

    @Schema(description = "Product name", example = "Custom Product Name")
    @NotBlank
    private String name;

    @Schema(description = "Product description")
    private String description;

    @Schema(description = "Short summary")
    private String summary;

    @Schema(description = "Product URL")
    private String url;

    @Schema(description = "Price information")
    private PriceWsDTO price;

    @Schema(description = "Stock availability status", example = "inStock")
    private String stockStatus;

    @Schema(description = "Available stock quantity")
    private Integer stockLevel;

    @Schema(description = "Whether product can be purchased")
    private Boolean purchasable;

    @Schema(description = "Product images")
    private List<ImageWsDTO> images;

    @Schema(description = "Primary image URL")
    private String imageUrl;

    @Schema(description = "Category code")
    private String categoryCode;

    @Schema(description = "Category name")
    private String categoryName;

    @Schema(description = "Brand name")
    private String brandName;

    @Schema(description = "Average customer rating", example = "4.5")
    private Double averageRating;

    @Schema(description = "Number of customer reviews")
    private Integer numberOfReviews;

    @Schema(description = "Custom field specific to this product type")
    private String customField;

    @Schema(description = "Custom status")
    private String customStatus;

    // Getters and Setters

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public PriceWsDTO getPrice() {
        return price;
    }

    public void setPrice(PriceWsDTO price) {
        this.price = price;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public Integer getStockLevel() {
        return stockLevel;
    }

    public void setStockLevel(Integer stockLevel) {
        this.stockLevel = stockLevel;
    }

    public Boolean getPurchasable() {
        return purchasable;
    }

    public void setPurchasable(Boolean purchasable) {
        this.purchasable = purchasable;
    }

    public List<ImageWsDTO> getImages() {
        return images;
    }

    public void setImages(List<ImageWsDTO> images) {
        this.images = images;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getNumberOfReviews() {
        return numberOfReviews;
    }

    public void setNumberOfReviews(Integer numberOfReviews) {
        this.numberOfReviews = numberOfReviews;
    }

    public String getCustomField() {
        return customField;
    }

    public void setCustomField(String customField) {
        this.customField = customField;
    }

    public String getCustomStatus() {
        return customStatus;
    }

    public void setCustomStatus(String customStatus) {
        this.customStatus = customStatus;
    }
}
