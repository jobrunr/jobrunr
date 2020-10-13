package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.utils.streams.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

public class JobCreationFilters extends AbstractJobFilters {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobCreationFilters.class);

    public JobCreationFilters(AbstractJob job, JobDefaultFilters jobDefaultFilters) {
        super(job, jobDefaultFilters);
    }

    public void runOnCreatingFilter() {
        jobClientFilters().forEach(catchThrowable(jobClientFilter -> jobClientFilter.onCreating(job)));
    }

    public void runOnCreatedFilter() {
        jobClientFilters().forEach(catchThrowable(jobClientFilter -> jobClientFilter.onCreated(job)));
    }

    private Stream<JobClientFilter> jobClientFilters() {
        return StreamUtils.ofType(jobFilters, JobClientFilter.class);
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }
}
