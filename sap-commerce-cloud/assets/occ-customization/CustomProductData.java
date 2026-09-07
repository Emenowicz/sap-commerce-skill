package com.example.facades.data;

/** Facade-layer product data extended with fields used by the custom OCC API. */
public class CustomProductData extends ProductData {

    private String customField;
    private String customStatus;

    public String getCustomField() {
        return customField;
    }

    public void setCustomField(final String customField) {
        this.customField = customField;
    }

    public String getCustomStatus() {
        return customStatus;
    }

    public void setCustomStatus(final String customStatus) {
        this.customStatus = customStatus;
    }
}
