package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobFilters;
import org.jobrunr.server.runner.BackgroundStaticJobWithoutIocRunner;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackgroundJobPerformerTest {

    private TestService testService;

    @Mock
    private BackgroundJobServer backgroundJobServer;
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private JobZooKeeper jobZooKeeper;

    private BackgroundJobTestFilter logAllStateChangesFilter;

    @BeforeEach
    void setUpTestService() {
        logAllStateChangesFilter = new BackgroundJobTestFilter();

        testService = new TestService();
        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        when(backgroundJobServer.getJobZooKeeper()).thenReturn(jobZooKeeper);
        when(backgroundJobServer.getJobFilters()).thenReturn(new JobFilters(logAllStateChangesFilter));
    }

    @Test
    void allStateChangesArePassingViaTheApplyStateFilterOnSuccess() {
        Job job = anEnqueuedJob().build();

        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(new BackgroundStaticJobWithoutIocRunner());

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("ENQUEUED->PROCESSING", "PROCESSING->SUCCEEDED");
        assertThat(logAllStateChangesFilter.processingPassed).isTrue();
        assertThat(logAllStateChangesFilter.processedPassed).isTrue();
    }

    @Test
    void allStateChangesArePassingViaTheApplyStateFilterOnFailure() {
        Job job = anEnqueuedJob().build();

        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(null);

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("ENQUEUED->PROCESSING", "PROCESSING->FAILED", "FAILED->SCHEDULED");
        assertThat(logAllStateChangesFilter.processingPassed).isTrue();
        assertThat(logAllStateChangesFilter.processedPassed).isFalse();
    }

}