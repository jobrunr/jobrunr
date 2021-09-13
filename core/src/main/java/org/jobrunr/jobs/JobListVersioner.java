package org.jobrunr.jobs;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class JobListVersioner implements AutoCloseable {

    private final List<JobVersioner> jobVersioners;

    public JobListVersioner(List<Job> jobs) {
        this.jobVersioners = jobs.stream().map(JobVersioner::new).collect(toList());
    }

    public boolean areNewJobs() {
        validateJobs();
        return jobVersioners.get(0).isNewJob();
    }

    public void validateJobs() {
        if(jobVersioners.get(0).isNewJob()) {
            if(! jobVersioners.stream().allMatch(JobVersioner::isNewJob)) {
                throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
            }
        } else {
            if(jobVersioners.stream().anyMatch(JobVersioner::isNewJob)) {
                throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
            }
        }
    }

    public void commitVersions() {
        this.jobVersioners.forEach(JobVersioner::commitVersion);
    }

    public void rollbackVersions(List<Job> jobsThatFailed) {
        Set<UUID> jobIdsThatFailed = jobsThatFailed.stream().map(Job::getId).collect(toSet());
        this.jobVersioners.stream()
                .filter(jobVersioner -> !jobIdsThatFailed.contains(jobVersioner.getJob().getId()))
                .forEach(JobVersioner::commitVersion);
    }

    @Override
    public void close() {
        this.jobVersioners.forEach(JobVersioner::close);
    }
}
