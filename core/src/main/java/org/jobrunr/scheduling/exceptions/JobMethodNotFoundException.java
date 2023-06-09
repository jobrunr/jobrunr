package org.jobrunr.scheduling.exceptions;

import org.jobrunr.jobs.JobDetails;

public class JobMethodNotFoundException extends JobNotFoundException {

    @SuppressWarnings("unused") // Needed for deserialization from FailedState
    public JobMethodNotFoundException(String message) {
        super(message);
    }

    public JobMethodNotFoundException(JobDetails jobDetails) {
        super(jobDetails);
    }
}
