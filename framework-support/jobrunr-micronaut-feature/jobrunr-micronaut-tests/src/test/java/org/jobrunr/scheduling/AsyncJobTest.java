package org.jobrunr.scheduling;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.micronaut.scheduling.AsyncJobTestService;
import org.jobrunr.micronaut.scheduling.AsyncJobTestServiceWithNestedJobService;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

@MicronautTest(rebuildContext = true)
public class AsyncJobTest {

    @Inject
    private AsyncJobTestService asyncJobTestService;
    @Inject
    private AsyncJobTestServiceWithNestedJobService asyncJobTestServiceWithNestedJobService;

    @Inject
    private StorageProvider storageProvider;

    @Test
    void testAsyncJob() {
        asyncJobTestService.testMethodAsAsyncJob();

        await().atMost(15, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) > 0);
        assertThat(storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(10)).get(0))
                .hasJobDetails(AsyncJobTestService.class, "testMethodAsAsyncJob");
    }

    @Test
    public void testNestedAsyncJob() {
        asyncJobTestServiceWithNestedJobService.testMethodThatCreatesOtherJobsAsAsyncJob();
        await().atMost(30, TimeUnit.SECONDS).until(() -> storageProvider.countJobs(StateName.SUCCEEDED) == 2);
    }

    @Test
    void testMethodIsNormallyInvokedWhenNotAnnotationWithJob() {
        var result = asyncJobTestService.classicMethod();
        assertThat(result).isEqualTo(2);
    }
}

