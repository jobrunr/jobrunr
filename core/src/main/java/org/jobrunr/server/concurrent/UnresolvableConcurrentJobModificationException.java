package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.utils.diagnostics.DiagnosticsBuilder;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.SevereJobRunrException.DiagnosticsAware;
import static org.jobrunr.utils.diagnostics.DiagnosticsBuilder.diagnostics;

public class UnresolvableConcurrentJobModificationException extends ConcurrentJobModificationException implements DiagnosticsAware {

    private final List<ConcurrentJobModificationResolveResult> concurrentJobModificationResolveResults;

    public UnresolvableConcurrentJobModificationException(List<ConcurrentJobModificationResolveResult> concurrentJobModificationResolveResults, Exception cause) {
        super(concurrentJobModificationResolveResults.stream().map(ConcurrentJobModificationResolveResult::getLocalJob).collect(toList()), cause);
        this.concurrentJobModificationResolveResults = concurrentJobModificationResolveResults;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "\n\n" + getDiagnosticsInfo().asMarkDown();
    }

    @Override
    public DiagnosticsBuilder getDiagnosticsInfo() {
        return diagnostics()
                .withTitle("Concurrent modified jobs:")
                .with(concurrentJobModificationResolveResults, (resolveResult, diagnosticsBuilder) -> appendDiagnosticsInfo(diagnosticsBuilder, resolveResult));
    }

    private void appendDiagnosticsInfo(DiagnosticsBuilder diagnostics, ConcurrentJobModificationResolveResult resolveResult) {
        Job localJob = resolveResult.getLocalJob();
        Job jobFromStorage = resolveResult.getJobFromStorage();

        diagnostics
                .withLine("Job id: " + localJob.getId())
                .withIndentedLine("Job Name: " + localJob.getJobName())
                .withIndentedLine("Job Signature: " + localJob.getJobSignature())
                .withIndentedLine("Local version: " + localJob.getVersion() + "; Storage version: " + jobFromStorage.getVersion())
                .withIndentedLine("Local state: " + getJobStates(localJob))
                .withIndentedLine("Storage state: " + getJobStates(jobFromStorage));
    }

    private String getJobStates(Job job) {
        StringBuilder result = new StringBuilder();
        final int jobStatesToShow = Math.min(6, job.getJobStates().size());
        for (int i = 1; i <= jobStatesToShow; i++) {
            final JobState jobState = job.getJobState(-i);
            result.append(jobState.getName());
            if (jobState instanceof ProcessingState) {
                result.append(" (updated at " + jobState.getUpdatedAt() + " on BackgroundJobServer " + ((ProcessingState) jobState).getServerId() + " and started at " + jobState.getCreatedAt() + ")");
            } else {
                result.append(" (at " + jobState.getUpdatedAt() + ")");
            }
            if (i < jobStatesToShow) {
                result.append(" ← ");
            }
        }
        return result.toString();
    }
}
