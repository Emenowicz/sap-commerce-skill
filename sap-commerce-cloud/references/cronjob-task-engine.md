# CronJobs & Task Engine

## Table of Contents
- [CronJob Model Architecture](#cronjob-model-architecture)
- [Implementing Custom Jobs](#implementing-custom-jobs)
- [Trigger Configuration](#trigger-configuration)
- [ImpEx Setup](#impex-setup)
- [Retry and Abort Handling](#retry-and-abort-handling)
- [Logging and Monitoring](#logging-and-monitoring)
- [Cloud (CCv2) Scheduling](#cloud-ccv2-scheduling)
- [Task Engine](#task-engine)

## CronJob Model Architecture

SAP Commerce uses a three-part model for scheduled jobs:

```
Job (what to do) → CronJob (execution instance) → Trigger (when to run)
```

### Key Types
- **Job**: Defines the logic (links to a `ServicelayerJob` or `PerformableJob`)
- **CronJob**: An execution instance with parameters, status, and logs
- **Trigger**: Schedule definition using cron expressions or relative timing

### CronJob Lifecycle States
```
UNKNOWN → RUNNING → FINISHED / ABORTED / PAUSED
                  → ERROR (on failure)
```

### Built-in CronJob Types
| Type | Purpose |
|------|---------|
| `CronJob` | Base type for simple jobs |
| `ImpExImportCronJob` | ImpEx data import |
| `SolrIndexerCronJob` | Solr indexing |
| `CleanUpCronJob` | Cleanup tasks |
| `CatalogVersionSyncCronJob` | Catalog sync |

## Implementing Custom Jobs

### AbstractJobPerformable (Standard Pattern)

The most common approach for custom scheduled logic:

```java
package com.example.jobs;

import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomCleanupJobPerformable extends AbstractJobPerformable<CronJobModel> {

    private static final Logger LOG = LoggerFactory.getLogger(CustomCleanupJobPerformable.class);

    @Override
    public PerformResult perform(final CronJobModel cronJobModel) {
        LOG.info("Starting custom cleanup job: {}", cronJobModel.getCode());

        try {
            // Business logic here
            processItems();

            return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
        } catch (final Exception e) {
            LOG.error("Cleanup job failed", e);
            return new PerformResult(CronJobResult.ERROR, CronJobStatus.ABORTED);
        }
    }

    private void processItems() {
        // Implementation
    }
}
```

### Custom CronJob with Parameters

For jobs that need custom configuration, define a custom CronJob type:

**items.xml**:
```xml
<itemtype code="DataExportCronJob"
          extends="CronJob"
          autocreate="true"
          generate="true">
    <attributes>
        <attribute qualifier="exportPath" type="java.lang.String">
            <persistence type="property"/>
        </attribute>
        <attribute qualifier="maxRecords" type="java.lang.Integer">
            <defaultvalue>Integer.valueOf(1000)</defaultvalue>
            <persistence type="property"/>
        </attribute>
    </attributes>
</itemtype>
```

**Typed JobPerformable**:
```java
public class DataExportJobPerformable extends AbstractJobPerformable<DataExportCronJobModel> {

    @Override
    public PerformResult perform(final DataExportCronJobModel cronJob) {
        final String exportPath = cronJob.getExportPath();
        final int maxRecords = cronJob.getMaxRecords();

        LOG.info("Exporting up to {} records to {}", maxRecords, exportPath);
        // Export logic using typed parameters
        return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
    }
}
```

### Abortable Jobs

Support graceful abort by checking `clearAbortRequestedIfNeeded()`:

```java
public class LongRunningJobPerformable extends AbstractJobPerformable<CronJobModel> {

    @Override
    public PerformResult perform(final CronJobModel cronJob) {
        final List<ItemModel> items = fetchItems();

        for (final ItemModel item : items) {
            if (clearAbortRequestedIfNeeded(cronJob)) {
                LOG.info("Job aborted by request");
                return new PerformResult(CronJobResult.UNKNOWN, CronJobStatus.ABORTED);
            }
            processItem(item);
        }
        return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
    }

    @Override
    public boolean isAbortable() {
        return true;
    }
}
```

## Trigger Configuration

### Cron Expressions

SAP Commerce uses standard cron expressions with 6 fields:

```
second minute hour day-of-month month day-of-week
```

| Expression | Schedule |
|-----------|----------|
| `0 0 * * * ?` | Every hour |
| `0 0 2 * * ?` | Daily at 2:00 AM |
| `0 0/30 * * * ?` | Every 30 minutes |
| `0 0 3 ? * MON-FRI` | Weekdays at 3:00 AM |
| `0 0 0 1 * ?` | First day of every month |

### ImpEx Trigger Setup (Classic)

```impex
INSERT_UPDATE Trigger; cronJob(code)[unique=true]; second; minute; hour; day; month; year; relative; active; maxAcceptableDelay
                     ; myCleanupCronJob          ; 0     ; 0     ; 3   ; -1 ; -1   ; -1  ; false   ; true  ; 600
```

### ImpEx Trigger Setup (Cron Expression)

```impex
INSERT_UPDATE Trigger; cronJob(code)[unique=true]; cronExpression  ; active
                     ; myCleanupCronJob          ; 0 0 3 * * ?     ; true
```

## ImpEx Setup

### Complete CronJob Configuration

```impex
# 1. Register the ServicelayerJob (links Spring bean to Job model)
INSERT_UPDATE ServicelayerJob; code[unique=true]   ; springId
                             ; customCleanupJob    ; customCleanupJobPerformable

# 2. Create the CronJob instance
INSERT_UPDATE CronJob; code[unique=true]    ; job(code)        ; sessionLanguage(isocode); singleExecutable; logToDatabase
                     ; customCleanupCronJob ; customCleanupJob ; en                     ; true            ; true

# 3. Create the Trigger (schedule)
INSERT_UPDATE Trigger; cronJob(code)[unique=true]; cronExpression  ; active
                     ; customCleanupCronJob      ; 0 0 2 * * ?     ; true
```

### Custom CronJob Type Setup

```impex
INSERT_UPDATE ServicelayerJob; code[unique=true]   ; springId
                             ; dataExportJob       ; dataExportJobPerformable

INSERT_UPDATE DataExportCronJob; code[unique=true]     ; job(code)     ; exportPath           ; maxRecords; sessionLanguage(isocode)
                               ; dailyDataExportCron   ; dataExportJob ; /tmp/export/daily    ; 5000      ; en

INSERT_UPDATE Trigger; cronJob(code)[unique=true]; cronExpression  ; active
                     ; dailyDataExportCron       ; 0 0 4 * * ?     ; true
```

## Retry and Abort Handling

### Retry on Failure

CronJobs don't have built-in retry. Implement retry in the job logic:

```java
public PerformResult perform(final CronJobModel cronJob) {
    int retries = 3;
    while (retries > 0) {
        try {
            doWork();
            return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
        } catch (final TransientException e) {
            retries--;
            LOG.warn("Attempt failed, {} retries remaining", retries, e);
            if (retries == 0) {
                return new PerformResult(CronJobResult.ERROR, CronJobStatus.ABORTED);
            }
        }
    }
    return new PerformResult(CronJobResult.ERROR, CronJobStatus.ABORTED);
}
```

### Error Notification

Set `errorMode` and `emailAddress` on the CronJob to receive notifications:

```impex
UPDATE CronJob; code[unique=true]    ; errorMode(code); emailAddress
              ; customCleanupCronJob ; FAIL           ; admin@example.com
```

## Logging and Monitoring

### CronJob Log Access

```java
// In a service or controller
final CronJobModel cronJob = cronJobService.getCronJob("customCleanupCronJob");
LOG.info("Status: {}", cronJob.getStatus());
LOG.info("Result: {}", cronJob.getResult());
LOG.info("Start time: {}", cronJob.getStartTime());
LOG.info("End time: {}", cronJob.getEndTime());
```

### Monitoring via HAC

Navigate to **Platform > CronJobs** in HAC to:
- View running, completed, and failed jobs
- Trigger manual execution
- View execution logs
- Abort running jobs

### FlexibleSearch for CronJob Status

```sql
-- Find all failed CronJobs in the last 24 hours
SELECT {code}, {result}, {startTime}, {endTime}
FROM {CronJob}
WHERE {result} = ({{SELECT {pk} FROM {CronJobResult} WHERE {code} = 'ERROR'}})
  AND {startTime} > ?yesterday
```

## Cloud (CCv2) Scheduling

On CCv2, cron jobs run on background processing nodes. Key considerations:

- **Node Affinity**: Jobs run on any available background node; use `nodeID` to pin to specific nodes if needed
- **Backoffice Scheduling**: Use Backoffice > System > Background Processes > CronJobs to manage schedules
- **Single Execution**: Set `singleExecutable=true` to prevent parallel runs across cluster nodes
- **Timeout**: Long-running jobs should check abort status regularly to avoid node-level timeouts

### CCv2 Best Practices

1. Always set `singleExecutable=true` for data-modifying jobs
2. Use `logToDatabase=true` for production monitoring
3. Keep job execution under deployment timeout limits
4. Test CronJobs in staging before enabling triggers in production

## Task Engine

The Task Engine is a lower-level alternative to CronJobs for asynchronous, one-off tasks.

### When to Use Task Engine vs CronJob

| Feature | CronJob | Task Engine |
|---------|---------|-------------|
| Scheduling | Recurring via triggers | One-off or programmatic |
| Monitoring | Backoffice/HAC | Limited |
| Cluster-aware | Via `singleExecutable` | Built-in |
| Use case | Scheduled batch jobs | Async event processing |

### Creating a Task

```java
final TaskModel task = modelService.create(TaskModel.class);
task.setRunnerBean("myTaskRunner");
task.setExecutionDate(new Date());
task.setContext("contextData");
modelService.save(task);
// Task engine picks it up automatically
```

### TaskRunner Implementation

```java
public class MyTaskRunner implements TaskRunner<TaskModel> {

    @Override
    public void run(final TaskService taskService, final TaskModel task) throws RetryLaterException {
        final String context = (String) task.getContext();
        // Process the task
    }

    @Override
    public void handleError(final TaskService taskService, final TaskModel task, final Throwable error) {
        LOG.error("Task {} failed: {}", task.getPk(), error.getMessage());
    }
}
```
