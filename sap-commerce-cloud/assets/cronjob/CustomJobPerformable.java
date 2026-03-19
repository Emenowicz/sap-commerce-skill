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
        LOG.info("Starting job: {}", cronJobModel.getCode());

        try {
            int processed = 0;
            int errors = 0;

            // TODO: Replace with actual business logic
            // Example: iterate over items and process them
            // final List<ItemModel> items = fetchItemsToProcess();
            // for (final ItemModel item : items) {
            //     if (clearAbortRequestedIfNeeded(cronJobModel)) {
            //         LOG.info("Job aborted by user request after processing {} items", processed);
            //         return new PerformResult(CronJobResult.UNKNOWN, CronJobStatus.ABORTED);
            //     }
            //     try {
            //         processItem(item);
            //         processed++;
            //     } catch (final Exception e) {
            //         LOG.error("Error processing item {}", item.getPk(), e);
            //         errors++;
            //     }
            // }

            LOG.info("Job completed. Processed: {}, Errors: {}", processed, errors);

            if (errors > 0) {
                return new PerformResult(CronJobResult.WARNING, CronJobStatus.FINISHED);
            }
            return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);

        } catch (final Exception e) {
            LOG.error("Job failed with unexpected error", e);
            return new PerformResult(CronJobResult.ERROR, CronJobStatus.ABORTED);
        }
    }

    @Override
    public boolean isAbortable() {
        return true;
    }
}
