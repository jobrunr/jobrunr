package org.jobrunr.storage;

import org.jobrunr.jobs.Job;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class ConcurrentJobModificationException extends StorageException {

    private final List<Job> concurrentUpdatedJobs;

    public ConcurrentJobModificationException(Job concurrentUpdatedJob) {
        this(singletonList(concurrentUpdatedJob));
    }

    public ConcurrentJobModificationException(List<Job> concurrentUpdatedJobs) {
        super("The following jobs where concurrently updated: " + concurrentUpdatedJobs.stream().map(job -> job.getId().toString()).collect(Collectors.joining(", ")));
        this.concurrentUpdatedJobs = concurrentUpdatedJobs;
    }

    public List<Job> getConcurrentUpdatedJobs() {
        return concurrentUpdatedJobs;
    }
}
