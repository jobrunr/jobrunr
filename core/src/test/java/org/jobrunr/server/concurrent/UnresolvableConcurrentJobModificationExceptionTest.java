package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.*;

class UnresolvableConcurrentJobModificationExceptionTest {

    @Test
    void canGenerateCorrectDiagnosticsInfo() {
        final Job localJob = aSucceededJob().build();
        final Job jobFromStorage = aFailedJob().build();

        final ConcurrentJobModificationResolveResult resolveResult = ConcurrentJobModificationResolveResult.failed(localJob, jobFromStorage);
        final UnresolvableConcurrentJobModificationException unresolvableConcurrentJobModificationException = new UnresolvableConcurrentJobModificationException(singletonList(resolveResult), new Exception());

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
        final UnresolvableConcurrentJobModificationException unresolvableConcurrentJobModificationException = new UnresolvableConcurrentJobModificationException(singletonList(resolveResult), new Exception());

        final String markDown = unresolvableConcurrentJobModificationException.getDiagnosticsInfo().asMarkDown();
        assertThat(markDown)
                .containsPattern("ENQUEUED")
                .containsPattern("FAILED (.*) ← PROCESSING (.*) ← ENQUEUED");
    }

    @Test
    void logsAllInfoAlsoToConsole() {
        final Job localJob = anEnqueuedJob().build();
        final Job jobFromStorage = aFailedJob().build();

        final ConcurrentJobModificationResolveResult resolveResult = ConcurrentJobModificationResolveResult.failed(localJob, jobFromStorage);
        final UnresolvableConcurrentJobModificationException unresolvableConcurrentJobModificationException = new UnresolvableConcurrentJobModificationException(singletonList(resolveResult), new Exception());

        assertThat(unresolvableConcurrentJobModificationException)
                .hasMessageContaining("Job Name: an enqueued job")
                .hasMessageContaining("Job Signature: java.lang.System.out.println(java.lang.String)")
                .hasMessageContaining("Local state: ENQUEUED")
                .hasMessageContaining("Storage state: FAILED");
    }
}