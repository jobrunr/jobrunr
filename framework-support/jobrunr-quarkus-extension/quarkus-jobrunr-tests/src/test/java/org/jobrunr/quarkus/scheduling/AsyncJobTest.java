package org.jobrunr.quarkus.scheduling;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

@QuarkusTest
@TestProfile(AsyncJobTest.class) // prevents conflict with other tests by starting with a clean context
public class AsyncJobTest implements QuarkusTestProfile {

    @Inject
    AsyncJobTestService asyncJobTestService;

    @Inject
    StorageProvider storageProvider;

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
