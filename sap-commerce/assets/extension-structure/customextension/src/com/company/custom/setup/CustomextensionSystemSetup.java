/*
 * CustomextensionSystemSetup.java
 * System setup class for creating essential and project data.
 * Runs during ant initialize and ant updatesystem.
 */
package com.company.custom.setup;

import com.company.custom.constants.CustomextensionConstants;

import de.hybris.platform.core.initialization.SystemSetup;
import de.hybris.platform.core.initialization.SystemSetup.Process;
import de.hybris.platform.core.initialization.SystemSetup.Type;
import de.hybris.platform.core.initialization.SystemSetupContext;
import de.hybris.platform.core.initialization.SystemSetupParameter;
import de.hybris.platform.core.initialization.SystemSetupParameterMethod;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * System setup for customextension.
 * Creates essential data during initialization and optional sample data.
 */
@SystemSetup(extension = CustomextensionConstants.EXTENSIONNAME)
public class CustomextensionSystemSetup {

    private static final Logger LOG = LoggerFactory.getLogger(CustomextensionSystemSetup.class);

    private ModelService modelService;
    private FlexibleSearchService flexibleSearchService;

    /**
     * Define setup parameters shown in HAC during update.
     */
    @SystemSetupParameterMethod
    public List<SystemSetupParameter> getSystemSetupParameters() {
        final List<SystemSetupParameter> params = new ArrayList<>();

        params.add(createBooleanSystemSetupParameter(
            "createSampleData",
            "Create Sample Data",
            true
        ));

        return params;
    }

    /**
     * Create essential data required for extension to function.
     * Runs during both initialize and update.
     */
    @SystemSetup(type = Type.ESSENTIAL, process = Process.ALL)
    public void createEssentialData() {
        LOG.info("Creating essential data for customextension...");

        // Create essential configuration, permissions, etc.
        // Example: Create required user groups, access rights

        LOG.info("Essential data creation complete.");
    }

    /**
     * Create project/sample data.
     * Runs during initialize and update if parameter is selected.
     */
    @SystemSetup(type = Type.PROJECT, process = Process.ALL)
    public void createProjectData(final SystemSetupContext context) {
        LOG.info("Creating project data for customextension...");

        // Check if sample data should be created
        if (context.getParameterMap() != null &&
            getBooleanParameter(context, "createSampleData")) {

            LOG.info("Creating sample data...");
            createSampleCustomItems();
        }

        LOG.info("Project data creation complete.");
    }

    /**
     * Create sample CustomItem instances.
     */
    private void createSampleCustomItems() {
        // Implementation would create sample data
        // Using modelService.create() and modelService.save()
        LOG.info("Sample CustomItems created.");
    }

    private boolean getBooleanParameter(final SystemSetupContext context, final String key) {
        return context.getParameterMap().containsKey(key) &&
               Boolean.TRUE.toString().equals(context.getParameterMap().get(key));
    }

    private SystemSetupParameter createBooleanSystemSetupParameter(
            final String key, final String label, final boolean defaultValue) {

        final SystemSetupParameter param = new SystemSetupParameter(key);
        param.setLabel(label);
        param.addValue("true", defaultValue);
        param.addValue("false", !defaultValue);
        return param;
    }

    // Setter injection
    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
}
