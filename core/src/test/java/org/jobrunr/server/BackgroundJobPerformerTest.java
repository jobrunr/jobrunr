package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.DisplayNameFilter;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.filters.JobFilters;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.server.runner.BackgroundStaticJobWithoutIocRunner;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackgroundJobPerformerTest {

    private TestService testService;

    @Mock
    private BackgroundJobServer backgroundJobServer;
    @Mock
    private StorageProvider storageProvider;

    @BeforeEach
    public void setUpTestService() {
        testService = new TestService();
        lenient().when(backgroundJobServer.getJobFilters()).thenReturn(new JobFilters(new DisplayNameFilter(), new RetryFilter()));
    }

    @Test
    public void allStateChangesArePassingViaTheApplyStateFilterOnSuccess() {
        BackgroundJobTestFilter logAllStateChangesFilter = new BackgroundJobTestFilter();
        Job job = anEnqueuedJob().build();

        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(new BackgroundStaticJobWithoutIocRunner());
        when(backgroundJobServer.getJobFilters()).thenReturn(new JobFilters(logAllStateChangesFilter));

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.call();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("ENQUEUED->PROCESSING", "PROCESSING->SUCCEEDED");
        assertThat(logAllStateChangesFilter.processingPassed).isTrue();
        assertThat(logAllStateChangesFilter.processedPassed).isTrue();
    }

    @Test
    public void allStateChangesArePassingViaTheApplyStateFilterOnFailure() {
        BackgroundJobTestFilter logAllStateChangesFilter = new BackgroundJobTestFilter();
        Job job = anEnqueuedJob().build();

        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(null);
        when(backgroundJobServer.getJobFilters()).thenReturn(new JobFilters(logAllStateChangesFilter));

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.call();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("ENQUEUED->PROCESSING", "PROCESSING->FAILED", "FAILED->SCHEDULED");
        assertThat(logAllStateChangesFilter.processingPassed).isTrue();
        assertThat(logAllStateChangesFilter.processedPassed).isFalse();
    }

    private List<JobFilter> getJobFilters(Job job) {
        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        return Whitebox.getInternalState(backgroundJobPerformer, "jobFilters");
    }

}