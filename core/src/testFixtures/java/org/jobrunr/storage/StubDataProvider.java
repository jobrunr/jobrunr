package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.ScheduledState;

import java.util.List;
import java.util.stream.IntStream;

import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
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
        for (int i = 0; i < 33; i++) {
            List<Job> jobs = IntStream.range(0, 1000).mapToObj(j -> anEnqueuedJobThatTakesLong().build()).collect(toList());
            storageProvider.save(jobs);
        }
        storageProvider.save(aJob().withState(new ScheduledState(now().plusSeconds(60L * 60 * 5))).build());
        storageProvider.save(aSucceededJob().build());
        storageProvider.save(aFailedJobWithRetries().build());
        storageProvider.save(aFailedJobThatEventuallySucceeded().build());
        return this;
    }

    public StubDataProvider addSomeRecurringJobs() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("import-sales-data").withName("Import all sales data at midnight").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("generate-sales-reports").withName("Generate sales report at 3am").withCronExpression("0 3 * * *").build());
        return this;
    }
}
