/*
 * CustomProductWsDTO.java
 * Web Service DTO for custom product API responses.
 * Uses Swagger annotations for API documentation.
 */
package com.example.dto;

import de.hybris.platform.commercewebservicescommons.dto.product.PriceWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.ImageWsDTO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

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
@ApiModel(value = "CustomProduct", description = "Custom product representation")
public class CustomProductWsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "Unique product code", required = true, example = "CUSTOM001")
    private String code;

    @ApiModelProperty(value = "Product name", example = "Custom Product Name")
    private String name;

    @ApiModelProperty(value = "Product description")
    private String description;

    @ApiModelProperty(value = "Short summary")
    private String summary;

    @ApiModelProperty(value = "Product URL")
    private String url;

    @ApiModelProperty(value = "Price information")
    private PriceWsDTO price;

    @ApiModelProperty(value = "Stock availability status", example = "inStock")
    private String stockStatus;

    @ApiModelProperty(value = "Available stock quantity")
    private Integer stockLevel;

    @ApiModelProperty(value = "Whether product can be purchased")
    private Boolean purchasable;

    @ApiModelProperty(value = "Product images")
    private List<ImageWsDTO> images;

    @ApiModelProperty(value = "Primary image URL")
    private String imageUrl;

    @ApiModelProperty(value = "Category code")
    private String categoryCode;

    @ApiModelProperty(value = "Category name")
    private String categoryName;

    @ApiModelProperty(value = "Brand name")
    private String brandName;

    @ApiModelProperty(value = "Average customer rating", example = "4.5")
    private Double averageRating;

    @ApiModelProperty(value = "Number of customer reviews")
    private Integer numberOfReviews;

    @ApiModelProperty(value = "Custom field specific to this product type")
    private String customField;

    @ApiModelProperty(value = "Custom status")
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
