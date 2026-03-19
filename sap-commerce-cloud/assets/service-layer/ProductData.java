/*
 * ProductData.java
 * Data Transfer Object (DTO) for product information.
 * DTOs carry data between facade and presentation layers.
 */
package com.example.facades.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object for Product.
 *
 * DTOs should NOT:
 * - Contain Model references
 * - Have business logic methods
 * - Reference platform classes
 *
 * Naming convention: *Data suffix (e.g., ProductData, CustomerData)
 */
public class ProductData implements Serializable {

    private static final long serialVersionUID = 1L;

    // Basic product information
    private String code;
    private String name;
    private String description;
    private String summary;
    private String url;

    // Images
    private String imageUrl;
    private String thumbnailUrl;
    private List<ImageData> images;

    // Price information
    private BigDecimal price;
    private String currencyIso;
    private String formattedPrice;

    // Stock information
    private Integer stockLevel;
    private String stockStatus;
    private boolean purchasable;

    // Category
    private String categoryCode;
    private String categoryName;
    private List<CategoryData> categories;

    // Brand
    private String brandName;

    // Reviews
    private Double averageRating;
    private Integer numberOfReviews;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public List<ImageData> getImages() {
        return images;
    }

    public void setImages(List<ImageData> images) {
        this.images = images;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrencyIso() {
        return currencyIso;
    }

    public void setCurrencyIso(String currencyIso) {
        this.currencyIso = currencyIso;
    }

    public String getFormattedPrice() {
        return formattedPrice;
    }

    public void setFormattedPrice(String formattedPrice) {
        this.formattedPrice = formattedPrice;
    }

    public Integer getStockLevel() {
        return stockLevel;
    }

    public void setStockLevel(Integer stockLevel) {
        this.stockLevel = stockLevel;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public boolean isPurchasable() {
        return purchasable;
    }

    public void setPurchasable(boolean purchasable) {
        this.purchasable = purchasable;
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

    public List<CategoryData> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryData> categories) {
        this.categories = categories;
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

    // Nested DTOs

    public static class ImageData implements Serializable {
        private static final long serialVersionUID = 1L;
        private String url;
        private String altText;
        private String format;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getAltText() { return altText; }
        public void setAltText(String altText) { this.altText = altText; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }

    public static class CategoryData implements Serializable {
        private static final long serialVersionUID = 1L;
        private String code;
        private String name;
        private String url;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
