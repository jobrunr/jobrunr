package org.jobrunr.quarkus.scheduling;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.Paging;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Test;

import static org.jobrunr.JobRunrAssertions.assertThat;

@QuarkusTest
public class AsyncJobTest {

    @Inject
    private AsyncJobTestService asyncJobTestService;

    @Inject
    private StorageProvider storageProvider;

    @Test
    void JobEnqueuedWhenCallingServiceWithAsyncJobAnnotation() {
        asyncJobTestService.createSomeJob();

        var jobsFromDb = storageProvider.getJobList(StateName.ENQUEUED, Paging.AmountBasedList.ascOnUpdatedAt(10));
        jobsFromDb.addAll(storageProvider.getJobList(StateName.PROCESSING, Paging.AmountBasedList.ascOnUpdatedAt(10)));
        jobsFromDb.addAll(storageProvider.getJobList(StateName.SUCCEEDED, Paging.AmountBasedList.ascOnUpdatedAt(10)));

        assertThat(jobsFromDb).hasSize(1);
        assertThat(jobsFromDb.get(0)).hasJobDetails(AsyncJobTestService.class, "createSomeJob");
    }

}
