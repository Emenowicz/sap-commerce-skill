package com.example.facades.impl;

import com.example.facades.CustomCheckoutFacade;

import de.hybris.platform.order.CartService;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.Collections;
import java.util.List;

/** Persists the option on the current cart; requires checkout-items.xml. */
public class DefaultCustomCheckoutFacade implements CustomCheckoutFacade {

    private static final String SELECTED_OPTION = "customCheckoutOption";

    private CartService cartService;
    private ModelService modelService;
    private List<String> availableOptions = Collections.emptyList();

    @Override
    public List<String> getAvailableOptions() {
        return Collections.unmodifiableList(availableOptions);
    }

    @Override
    public String getSelectedCustomOption() {
        return cartService.hasSessionCart()
                ? modelService.getAttributeValue(cartService.getSessionCart(), SELECTED_OPTION)
                : null;
    }

    @Override
    public void saveCustomOption(final String optionCode) {
        if (!isValidOption(optionCode)) {
            throw new IllegalArgumentException("Unsupported checkout option: " + optionCode);
        }
        if (!cartService.hasSessionCart()) {
            throw new IllegalStateException("An existing cart is required");
        }
        final CartModel cart = cartService.getSessionCart();
        modelService.setAttributeValue(cart, SELECTED_OPTION, optionCode);
        modelService.save(cart);
    }

    @Override
    public boolean isValidOption(final String optionCode) {
        return optionCode != null && availableOptions.contains(optionCode);
    }

    @Override
    public boolean isCustomStepRequired() {
        return !availableOptions.isEmpty();
    }

    public void setCartService(final CartService cartService) {
        this.cartService = cartService;
    }

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setAvailableOptions(final List<String> availableOptions) {
        this.availableOptions = List.copyOf(availableOptions);
    }
}
