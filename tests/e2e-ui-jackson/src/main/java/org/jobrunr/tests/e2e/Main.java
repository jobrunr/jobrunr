package org.jobrunr.tests.e2e;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

import static java.time.Instant.now;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobThatEventuallySucceeded;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;

public class Main extends AbstractMain {

    public static void main(String[] args) throws Exception {
        new Main(args);
    }

    public Main(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected StorageProvider initStorageProvider() {
        final org.jobrunr.storage.InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        addDefaultData(storageProvider);
        return storageProvider;
    }

    // see https://github.com/eclipse/buildship/issues/991
    public void addDefaultData(StorageProvider storageProvider) {
        final BackgroundJobServerStatus backgroundJobServerStatus = aDefaultBackgroundJobServerStatus().withPollIntervalInSeconds(10).build();
        backgroundJobServerStatus.start();
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
    }
}
