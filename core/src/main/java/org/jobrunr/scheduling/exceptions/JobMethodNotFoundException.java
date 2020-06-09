package org.jobrunr.scheduling.exceptions;

import org.jobrunr.jobs.JobDetails;

public class JobMethodNotFoundException extends JobNotFoundException {

    public JobMethodNotFoundException(JobDetails jobDetails) {
        super(jobDetails);
    }

    public JobMethodNotFoundException(String message) {
        super(message);
    }
}
