package org.jobrunr.scheduling;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.micronaut.scheduling.AsyncJobTestService;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Test;

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
    private StorageProvider storageProvider;

    @Test
    void jobIsEnqueuedWhenCallingServiceWithAsyncJobAnnotation() {
        asyncJobTestService.runSomeJob();

        await().atMost(15, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) > 0);
        assertThat(storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(10)).get(0))
                .hasJobDetails(AsyncJobTestService.class, "runSomeJob");
    }

    @Test
    void methodIsNormallyInvokedWhenNotAnnotationWithJob() {
        int res = asyncJobTestService.classicMethod();

        assertThat(res).isEqualTo(2);
    }
}
