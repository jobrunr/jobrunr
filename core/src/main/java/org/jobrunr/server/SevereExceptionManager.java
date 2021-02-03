package org.jobrunr.server;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.ThreadSafeStorageProvider;
import org.jobrunr.utils.RuntimeUtils;
import org.jobrunr.utils.metadata.VersionRetriever;

import java.time.Instant;

import static java.lang.String.format;
import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.diagnostics.DiagnosticsBuilder.diagnostics;

public class SevereExceptionManager {

    private final String id;
    private final StorageProvider storageProvider;

    public SevereExceptionManager(BackgroundJobServer backgroundJobServer) {
        this.id = "BackgroundJobServer " + backgroundJobServer.getId().toString();
        this.storageProvider = backgroundJobServer.getStorageProvider();
    }

    public void handle(SevereJobRunrException e) {
        this.storageProvider.saveMetadata(exceptionToJobRunrMetadata(e));
    }

    private JobRunrMetadata exceptionToJobRunrMetadata(SevereJobRunrException e) {
        String diagnosticsInfo = diagnostics()
                .withTitle(e.getClass().getSimpleName() + " occurred in " + id + (isNotNullOrEmpty(e.getMessage()) ? ": " + e.getMessage() : ""))
                .withEmptyLine()
                .withSubTitle("Runtime information")
                .withBulletedLine("Timestamp", Instant.now().toString())
                .withBulletedLine("Location", id)
                .withBulletedLine("JobRunr Version", VersionRetriever.getVersion(JobRunr.class))
                .withBulletedLine("StorageProvider", storageProvider instanceof ThreadSafeStorageProvider ? ((ThreadSafeStorageProvider) storageProvider).getStorageProvider().getClass().getName() : storageProvider.getClass().getName())
                .withBulletedLine("Java Version", System.getProperty("java.version"))
                .withBulletedLine("Is running from nested jar", Boolean.toString(RuntimeUtils.isRunningFromNestedJar()))
                .withEmptyLine()
                .withSubTitle("Background Job Servers")
                .with(storageProvider.getBackgroundJobServers(), (server, diagnosticsBuilder) -> diagnosticsBuilder.withBulletedLine(format("BackgroundJobServer id: %s\n(workerPoolSize: %d, pollIntervalInSeconds: %d, firstHeartbeat: %s, lastHeartbeat: %s)", server.getId(), server.getWorkerPoolSize(), server.getPollIntervalInSeconds(), server.getFirstHeartbeat(), server.getLastHeartbeat())))
                .withEmptyLine()
                .withSubTitle("Diagnostics from exception")
                .withDiagnostics(2, e.getDiagnostics())
                .withEmptyLine()
                .withSubTitle("Exception")
                .withException(e)
                .AsMarkDown();

        return new JobRunrMetadata(SevereJobRunrException.class.getSimpleName(), id, diagnosticsInfo);
    }
}
