package org.jobrunr.jobs.details.postprocess;

import org.jobrunr.jobs.JobDetails;

public interface JobDetailsPostProcessor {

    JobDetails postProcess(JobDetails jobDetails);

}
