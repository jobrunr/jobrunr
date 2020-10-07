package org.jobrunr.jobs.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JobDefaultFilters {

    private final List<JobFilter> filters;

    public JobDefaultFilters(JobFilter... filters) {
        this(Arrays.asList(filters));
    }

    public JobDefaultFilters(List<JobFilter> filters) {
        this.filters = getAllJobFilters(filters);
    }

    List<JobFilter> getFilters() {
        return filters;
    }

    private ArrayList<JobFilter> getAllJobFilters(List<JobFilter> jobFilters) {
        final ArrayList<JobFilter> result = new ArrayList<>(Arrays.asList(new DisplayNameFilter(), new RetryFilter()));
        result.addAll(jobFilters);
        return result;
    }
}
