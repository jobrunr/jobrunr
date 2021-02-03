package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.utils.diagnostics.DiagnosticsBuilder;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.SevereJobRunrException.DiagnosticsAware;
import static org.jobrunr.utils.diagnostics.DiagnosticsBuilder.diagnostics;

public class UnresolvableConcurrentJobModificationException extends ConcurrentJobModificationException implements DiagnosticsAware {

    private final List<ConcurrentJobModificationResolveResult> concurrentJobModificationResolveResults;

    public UnresolvableConcurrentJobModificationException(List<ConcurrentJobModificationResolveResult> concurrentJobModificationResolveResults) {
        super(concurrentJobModificationResolveResults.stream().map(ConcurrentJobModificationResolveResult::getLocalJob).collect(toList()));
        this.concurrentJobModificationResolveResults = concurrentJobModificationResolveResults;
    }

    @Override
    public DiagnosticsBuilder getDiagnosticsInfo() {
        return diagnostics()
                .withTitle("Concurrent modified jobs:")
                .with(concurrentJobModificationResolveResults, ((resolveResult, diagnosticsBuilder) -> appendDiagnosticsInfo(diagnosticsBuilder, resolveResult)));
    }

    private void appendDiagnosticsInfo(DiagnosticsBuilder diagnostics, ConcurrentJobModificationResolveResult resolveResult) {
        Job localJob = resolveResult.getLocalJob();
        Job jobFromStorage = resolveResult.getJobFromStorage();

        diagnostics
                .withLine("Job id: " + localJob.getId())
                .withIndentedLine("Local version: " + localJob.getVersion() + "; Storage version: " + jobFromStorage.getVersion())
                .withIndentedLine("Local state: " + localJob.getState() + "; Storage state: " + jobFromStorage.getState())
                .withIndentedLine("Local last updated: " + localJob.getUpdatedAt() + "; Storage last updated: " + jobFromStorage.getUpdatedAt());
    }
}
