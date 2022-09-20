package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.ScheduledState;

import java.util.List;
import java.util.stream.IntStream;

import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.jobs.JobTestBuilder.*;
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
        for (int i = 0; i < 14; i++) {
            List<Job> jobs = IntStream.range(0, 10000).mapToObj(j -> anEnqueuedJobThatTakesLong().build()).collect(toList());
            storageProvider.save(jobs);
            System.out.println("Saved " + (i+1) * 10000 + " jobs");
        }
        storageProvider.save(aJob().withState(new ScheduledState(now().plusSeconds(60L * 60 * 5))).build());
        storageProvider.save(aSucceededJob().build());
        storageProvider.save(aFailedJobWithRetries().build());
        storageProvider.save(aFailedJobThatEventuallySucceeded().build());
        return this;
    }

    public StubDataProvider addSomeRecurringJobs() {
        for(int i = 0; i < 100; i++) {
            storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("import-sales-data-" + i).withName("Import all sales data at midnight").build());
            storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("generate-sales-reports-" + i).withName("Generate sales report at 3am").withCronExpression("0 0 3 * *").build());
        }
        return this;
    }
}
