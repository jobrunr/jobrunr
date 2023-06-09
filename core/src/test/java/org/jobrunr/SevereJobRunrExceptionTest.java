package org.jobrunr;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.jobrunr.server.concurrent.UnresolvableConcurrentJobModificationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class SevereJobRunrExceptionTest {

    @Test
    void severeJobRunrExceptionRootCauseIsPassedIfDiagnosticAwareIsAnException() {
        Job localJob = anEnqueuedJob().withVersion(3).build();
        Job jobFromStorage = aCopyOf(localJob).withVersion(3).build();


        SevereJobRunrException severeJobRunrException = new SevereJobRunrException("Could not resolve ConcurrentJobModificationException",
                new UnresolvableConcurrentJobModificationException(List.of(ConcurrentJobModificationResolveResult.failed(localJob, jobFromStorage))));

        assertThat(severeJobRunrException)
                .hasMessage("Could not resolve ConcurrentJobModificationException")
                .hasRootCauseInstanceOf(UnresolvableConcurrentJobModificationException.class);
    }
}