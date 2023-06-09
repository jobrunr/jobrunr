package org.jobrunr.storage;

import org.jobrunr.jobs.Job;

import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;

public class ConcurrentJobModificationException extends StorageException {

    private final List<Job> concurrentUpdatedJobs;

    public ConcurrentJobModificationException(Job concurrentUpdatedJob) {
        this(concurrentUpdatedJob, null);
    }

    public ConcurrentJobModificationException(Job concurrentUpdatedJob, Exception cause) {
        this(singletonList(concurrentUpdatedJob), cause);
    }

    public ConcurrentJobModificationException(List<Job> concurrentUpdatedJobs) {
        this(concurrentUpdatedJobs, null);
    }

    public ConcurrentJobModificationException(List<Job> concurrentUpdatedJobs, Exception cause) {
        super("The following jobs where concurrently updated: " + constructMessage(concurrentUpdatedJobs), cause);
        this.concurrentUpdatedJobs = concurrentUpdatedJobs;
    }

    public List<Job> getConcurrentUpdatedJobs() {
        return concurrentUpdatedJobs;
    }

    private static String constructMessage(List<Job> concurrentUpdatedJobs) {
        return concurrentUpdatedJobs.stream().map(job -> job.getId().toString()).collect(joining(", "));
    }
}
