package org.jobrunr.scheduling.exceptions;

import org.jobrunr.jobs.JobDetails;

public class JobClassNotFoundException extends JobNotFoundException {

    public JobClassNotFoundException(JobDetails jobDetails) {
        super(jobDetails);
    }
}
