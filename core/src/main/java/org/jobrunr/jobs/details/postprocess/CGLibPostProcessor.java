package org.jobrunr.jobs.details.postprocess;

import org.jobrunr.jobs.JobDetails;

import static org.jobrunr.utils.StringUtils.substringBefore;

public class CGLibPostProcessor implements JobDetailsPostProcessor {

    @Override
    public JobDetails postProcess(JobDetails jobDetails) {
        if (jobDetails.getClassName().matches("(.*)\\$\\$EnhancerBy(.*)CGLIB(.*)")) {
            return new JobDetails(
                    substringBefore(jobDetails.getClassName(), "$$EnhancerBy"),
                    jobDetails.getStaticFieldName().orElse(null),
                    jobDetails.getMethodName(),
                    jobDetails.getJobParameters()
            );
        }
        return jobDetails;
    }
}
