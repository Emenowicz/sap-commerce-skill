package com.example.jobs;

import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom job performable template.
 *
 * Replace CronJobModel with a custom CronJob type if parameters are needed.
 * Register as a Spring bean and link via ServicelayerJob in ImpEx.
 */
public class CustomJobPerformable extends AbstractJobPerformable<CronJobModel> {

    private static final Logger LOG = LoggerFactory.getLogger(CustomJobPerformable.class);

    @Override
    public PerformResult perform(final CronJobModel cronJobModel) {
        LOG.error("Job {} has no cleanup implementation; no work was performed", cronJobModel.getCode());
        // Implement and test the cleanup before enabling its trigger.
        return new PerformResult(CronJobResult.ERROR, CronJobStatus.ABORTED);
    }

    @Override
    public boolean isAbortable() {
        // Change to true only after the processing loop checks the abort flag.
        return false;
    }
}
