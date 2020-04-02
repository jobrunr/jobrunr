package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.AbstractJob;

public interface JobClientFilter extends JobFilter {

    void onCreating(AbstractJob job);

    void onCreated(AbstractJob job);

}
