package org.jobrunr.storage;

import org.jobrunr.jobs.states.ScheduledState;

import static java.time.Instant.now;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobThatEventuallySucceeded;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJobThatTakesLong;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;

public class StubDataProvider {

    private final StorageProvider storageProvider;

    private StubDataProvider(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    public static StubDataProvider using(StorageProvider storageProvider) {
        return new StubDataProvider(storageProvider);
    }

    public StubDataProvider addALotOfEnqueuedJobsThatTakeSomeTime() {
        for (int i = 0; i < 33000; i++) {
            storageProvider.save(anEnqueuedJobThatTakesLong().build());
        }
        storageProvider.save(aJob().withState(new ScheduledState(now().plusSeconds(60L * 60 * 5))).build());
        storageProvider.save(aSucceededJob().withoutId().build());
        storageProvider.save(aFailedJobWithRetries().withoutId().build());
        storageProvider.save(aFailedJobThatEventuallySucceeded().withoutId().build());
        return this;
    }

    public StubDataProvider addSomeRecurringJobs() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("import-sales-data").withName("Import all sales data at midnight").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("generate-sales-reports").withName("Generate sales report at 3am").withCronExpression("0 3 * * *").build());
        return this;
    }
}
