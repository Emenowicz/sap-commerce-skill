/*
 * CustomCheckoutFacade.java
 * Facade for custom checkout operations.
 */
package com.company.facades;

import java.util.List;

/**
 * Facade interface for custom checkout step operations.
 */
public interface CustomCheckoutFacade {

    /**
     * Get available custom options for the checkout step.
     * @return list of option codes
     */
    List<String> getAvailableOptions();

    /**
     * Get the currently selected custom option for the cart.
     * @return selected option code or null
     */
    String getSelectedCustomOption();

    /**
     * Save the selected custom option to the cart.
     * @param optionCode the selected option code
     */
    void saveCustomOption(String optionCode);

    /**
     * Validate if the given option is valid for selection.
     * @param optionCode the option to validate
     * @return true if valid
     */
    boolean isValidOption(String optionCode);

    /**
     * Check if custom step is required for current cart.
     * @return true if step should be shown
     */
    boolean isCustomStepRequired();
}
