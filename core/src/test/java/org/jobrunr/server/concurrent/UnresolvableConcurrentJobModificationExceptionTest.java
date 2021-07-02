package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class UnresolvableConcurrentJobModificationExceptionTest {

    @Test
    void canGenerateCorrectDiagnosticsInfo() {
        final Job localJob = aSucceededJob().build();
        final Job jobFromStorage = aFailedJob().build();

        final ConcurrentJobModificationResolveResult resolveResult = ConcurrentJobModificationResolveResult.failed(localJob, jobFromStorage);
        final UnresolvableConcurrentJobModificationException unresolvableConcurrentJobModificationException = new UnresolvableConcurrentJobModificationException(singletonList(resolveResult));

        final String markDown = unresolvableConcurrentJobModificationException.getDiagnosticsInfo().asMarkDown();
        assertThat(markDown)
                .containsPattern("SUCCEEDED (.*) ← PROCESSING (.*) ← ENQUEUED")
                .containsPattern("FAILED (.*) ← PROCESSING (.*) ← ENQUEUED");
    }

    @Test
    void canGenerateCorrectDiagnosticsInfoEvenWithOnly1State() {
        final Job localJob = anEnqueuedJob().build();
        final Job jobFromStorage = aFailedJob().build();

        final ConcurrentJobModificationResolveResult resolveResult = ConcurrentJobModificationResolveResult.failed(localJob, jobFromStorage);
        final UnresolvableConcurrentJobModificationException unresolvableConcurrentJobModificationException = new UnresolvableConcurrentJobModificationException(singletonList(resolveResult));

        final String markDown = unresolvableConcurrentJobModificationException.getDiagnosticsInfo().asMarkDown();
        assertThat(markDown)
                .containsPattern("ENQUEUED")
                .containsPattern("FAILED (.*) ← PROCESSING (.*) ← ENQUEUED");
    }

}