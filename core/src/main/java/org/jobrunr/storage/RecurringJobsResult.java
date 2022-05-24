package org.jobrunr.storage;

import org.jobrunr.jobs.RecurringJob;

import java.util.ArrayList;
import java.util.Collection;

public class RecurringJobsResult extends ArrayList<RecurringJob> {

    private final long recurringJobsLastModifiedHash;

    public RecurringJobsResult() {
        this(new ArrayList<>());
    }

    public RecurringJobsResult(Collection<RecurringJob> recurringJobs) {
        super(recurringJobs);
        this.recurringJobsLastModifiedHash = this.stream()
                .map(recurringJob -> recurringJob.getCreatedAt().toEpochMilli()).reduce(Long::sum)
                .orElse(-1L);
    }

    public long getLastModifiedHash() {
        return recurringJobsLastModifiedHash;
    }

    @Override
    public boolean add(RecurringJob recurringJob) {
        throw new UnsupportedOperationException("RecurringJobsResult is an unmodifiable list");
    }

    @Override
    public void add(int index, RecurringJob element) {
        throw new UnsupportedOperationException("RecurringJobsResult is an unmodifiable list");
    }

    @Override
    public RecurringJob remove(int index) {
        throw new UnsupportedOperationException("RecurringJobsResult is an unmodifiable list");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("RecurringJobsResult is an unmodifiable list");
    }

    @Override
    public boolean addAll(Collection<? extends RecurringJob> c) {
        throw new UnsupportedOperationException("RecurringJobsResult is an unmodifiable list");
    }

    @Override
    public boolean addAll(int index, Collection<? extends RecurringJob> c) {
        throw new UnsupportedOperationException("RecurringJobsResult is an unmodifiable list");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("RecurringJobsResult is an unmodifiable list");
    }
}
