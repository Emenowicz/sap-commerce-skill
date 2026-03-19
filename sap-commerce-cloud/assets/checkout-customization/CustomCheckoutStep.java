/*
 * CustomCheckoutStep.java
 * Custom checkout step controller for accelerator checkout flow.
 */
package com.example.storefront.controllers.pages.checkout.steps;

import com.example.facades.CustomCheckoutFacade;
import com.example.storefront.forms.CustomStepForm;

import de.hybris.platform.acceleratorstorefrontcommons.annotations.PreValidateCheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.CheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.constants.WebConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.checkout.steps.AbstractCheckoutStepController;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * Controller for custom checkout step.
 * Integrates with Spring Web Flow checkout process.
 */
@Controller
@RequestMapping("/checkout/multi/custom-step")
public class CustomCheckoutStepController extends AbstractCheckoutStepController {

    private static final String CUSTOM_STEP = "custom-step";
    private static final String CUSTOM_STEP_CMS_PAGE = "customCheckoutStepPage";

    @Resource
    private CustomCheckoutFacade customCheckoutFacade;

    /**
     * Enter the custom checkout step.
     * Load any required data and display the step form.
     */
    @Override
    @RequestMapping(method = RequestMethod.GET)
    @RequireHardLogIn
    @PreValidateCheckoutStep(checkoutStep = CUSTOM_STEP)
    public String enterStep(final Model model, final RedirectAttributes redirectAttributes)
            throws CMSItemNotFoundException {

        // Load existing data if available
        final CustomStepForm form = new CustomStepForm();
        form.setCustomOption(customCheckoutFacade.getSelectedCustomOption());

        model.addAttribute("customStepForm", form);
        model.addAttribute("customOptions", customCheckoutFacade.getAvailableOptions());

        // CMS page setup
        storeCmsPageInModel(model, getContentPageForLabelOrId(CUSTOM_STEP_CMS_PAGE));
        setUpMetaDataForContentPage(model, getContentPageForLabelOrId(CUSTOM_STEP_CMS_PAGE));
        model.addAttribute(WebConstants.BREADCRUMBS_KEY, getResourceBreadcrumbBuilder()
                .getBreadcrumbs("checkout.multi.customStep.breadcrumb"));

        return getViewForPage(model);
    }

    /**
     * Process the custom step form submission.
     * Validate and save data, then proceed to next step.
     */
    @RequestMapping(method = RequestMethod.POST)
    @RequireHardLogIn
    public String submitStep(@Valid final CustomStepForm form, final BindingResult bindingResult,
                             final Model model, final RedirectAttributes redirectAttributes)
            throws CMSItemNotFoundException {

        // Validate form
        if (bindingResult.hasErrors()) {
            return enterStep(model, redirectAttributes);
        }

        // Custom validation
        if (!customCheckoutFacade.isValidOption(form.getCustomOption())) {
            bindingResult.rejectValue("customOption", "checkout.custom.invalid.option");
            return enterStep(model, redirectAttributes);
        }

        // Save the custom data
        try {
            customCheckoutFacade.saveCustomOption(form.getCustomOption());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "checkout.custom.save.error");
            return enterStep(model, redirectAttributes);
        }

        // Proceed to next step
        return getCheckoutStep().nextStep();
    }

    /**
     * Go back to previous checkout step.
     */
    @RequestMapping(value = "/back", method = RequestMethod.GET)
    @RequireHardLogIn
    public String back(final RedirectAttributes redirectAttributes) {
        return getCheckoutStep().previousStep();
    }

    @Override
    protected CheckoutStep getCheckoutStep() {
        return getCheckoutStep(CUSTOM_STEP);
    }

    // Setter for testing
    public void setCustomCheckoutFacade(CustomCheckoutFacade customCheckoutFacade) {
        this.customCheckoutFacade = customCheckoutFacade;
    }
}
