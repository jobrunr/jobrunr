package org.jobrunr.jobs.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jobrunr.utils.CollectionUtils.isNullOrEmpty;

public class JobDefaultFilters {

    private final List<JobFilter> filters;

    public JobDefaultFilters(JobFilter... filters) {
        this(Arrays.asList(filters));
    }

    public JobDefaultFilters(List<JobFilter> filters) {
        this.filters = getAllJobFilters(filters);
    }

    public void addAll(List<? extends JobFilter> filters) {
        if (isNullOrEmpty(filters)) return;
        this.filters.addAll(filters);
    }

    List<JobFilter> getFilters() {
        return filters;
    }

    // TODO not used, can we remove it?
    public <T extends JobFilter> T getFilterOfType(Class<T> filterClass) {
        for (JobFilter filter : filters) {
            if (filterClass.isInstance(filter)) {
                return filterClass.cast(filter);
            }
        }
        return null;
    }

    private List<JobFilter> getAllJobFilters(List<JobFilter> jobFilters) {
        final ArrayList<JobFilter> result = new ArrayList<>(Arrays.asList(new DefaultJobFilter(), new RetryFilter()));
        result.addAll(jobFilters);
        return result;
    }
}
