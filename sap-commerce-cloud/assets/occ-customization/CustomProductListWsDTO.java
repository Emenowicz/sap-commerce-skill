package com.example.dto;

import java.io.Serializable;
import java.util.List;

/** Response wrapper for paginated custom-product results. */
public class CustomProductListWsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CustomProductWsDTO> products;
    private long totalCount;

    public List<CustomProductWsDTO> getProducts() {
        return products;
    }

    public void setProducts(final List<CustomProductWsDTO> products) {
        this.products = products;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(final long totalCount) {
        this.totalCount = totalCount;
    }
}
