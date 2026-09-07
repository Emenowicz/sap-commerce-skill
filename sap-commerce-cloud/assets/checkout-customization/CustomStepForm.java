package com.example.storefront.forms;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/** Form backing object for the custom checkout step. */
public class CustomStepForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    private String customOption;

    public String getCustomOption() {
        return customOption;
    }

    public void setCustomOption(final String customOption) {
        this.customOption = customOption;
    }

}
