package org.jobrunr.quarkus.scheduling;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
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
    AsyncJobTestServiceWithNestedJobService asyncJobTestServiceWithNestedJobService;
    @Inject
    private StorageProvider storageProvider;

    @BeforeEach
    void setUp() {
        ((InMemoryStorageProvider) this.storageProvider).clear();
    }

    @Test
    void jobIsEnqueuedWhenCallingServiceWithAsyncJobAnnotation() {
        asyncJobTestService.testMethodAsAsyncJob();

        await().atMost(15, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) > 0);
        assertThat(storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(10)).get(0))
                .hasJobDetails(AsyncJobTestService.class, "testMethodAsAsyncJob");
    }

    @Test
    void testNestedAsyncJob() {
        asyncJobTestServiceWithNestedJobService.testMethodThatCreatesOtherJobsAsAsyncJob();
        await().atMost(30, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == 2);
    }

    @Test
    void methodIsNormallyInvokedWhenNotAnnotationWithJob() {
        assertThat(asyncJobTestService.classicMethod()).isEqualTo(2);
    }
}
