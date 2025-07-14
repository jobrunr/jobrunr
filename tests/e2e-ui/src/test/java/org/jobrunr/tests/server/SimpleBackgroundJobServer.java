package org.jobrunr.tests.server;

import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;

import static java.time.Instant.now;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobThatEventuallySucceeded;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;

public class SimpleBackgroundJobServer extends AbstractSimpleBackgroundJobServer {
    @Override
    protected StorageProvider initStorageProvider() {
        return new InMemoryStorageProvider();
    }

    @Override
    protected void loadDefaultData(StorageProvider storageProvider) {
        final BackgroundJobServerStatus backgroundJobServerStatus = aDefaultBackgroundJobServerStatus()
                .withPollIntervalInSeconds(10)
                .withIsStarted()
                .build();
        storageProvider.announceBackgroundJobServer(backgroundJobServerStatus);
        for (int i = 0; i < 33; i++) {
            storageProvider.save(anEnqueuedJob().build());
        }
        storageProvider.save(aJob().withState(new ScheduledState(now().plusSeconds(60L * 60 * 5))).build());
        storageProvider.save(aFailedJobWithRetries().build());
        storageProvider.save(aFailedJobThatEventuallySucceeded().build());
        storageProvider.save(aSucceededJob().build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("import-sales-data").withName("Import all sales data at midnight").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("generate-sales-reports").withName("Generate sales report at 3am").withCronExpression("0 3 * * *").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("carbon-aware-generate-sales-reports").withName("Generate sales report at 3am with some margin for carbon aware scheduling").withCronExpression("0 3 * * * [PT2H/PT3H]").build());
    }
}
