package org.jobrunr.scheduling;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.Paging;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Test;

import static org.jobrunr.JobRunrAssertions.assertThat;

@MicronautTest
public class AsyncJobTest {

    @Inject
    private AsyncJobTestService asyncJobTestService;

    @Inject
    private StorageProvider storageProvider;
    
    @Test
    void JobEnqueuedWhenCallingServiceWithAsyncJobAnnotation() {
        asyncJobTestService.createSomeJob();

        // In case the build takes a bit longer: should be in any of these states.
        var jobsFromDb = storageProvider.getJobList(StateName.ENQUEUED, Paging.AmountBasedList.ascOnUpdatedAt(10));
        jobsFromDb.addAll(storageProvider.getJobList(StateName.PROCESSING, Paging.AmountBasedList.ascOnUpdatedAt(10)));
        jobsFromDb.addAll(storageProvider.getJobList(StateName.SUCCEEDED, Paging.AmountBasedList.ascOnUpdatedAt(10)));

        assertThat(jobsFromDb).hasSizeGreaterThan(0);
        assertThat(jobsFromDb.get(0)).hasJobDetails(AsyncJobTestService.class, "createSomeJob");
    }

}
