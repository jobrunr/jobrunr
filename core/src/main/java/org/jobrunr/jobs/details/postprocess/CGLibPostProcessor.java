package org.jobrunr.jobs.details.postprocess;

import org.jobrunr.jobs.JobDetails;

import static org.jobrunr.utils.StringUtils.*;

public class CGLibPostProcessor implements JobDetailsPostProcessor {

    @Override
    public JobDetails postProcess(JobDetails jobDetails) {
        if (isNotNullOrEmpty(substringBetween(jobDetails.getClassName(), "$$", "$$"))) {
            return new JobDetails(
                    substringBefore(jobDetails.getClassName(), "$$"),
                    jobDetails.getStaticFieldName(),
                    jobDetails.getMethodName(),
                    jobDetails.getJobParameters()
            );
        }
        return jobDetails;
    }
}
