package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.utils.JobUtils;
import org.jobrunr.utils.StringUtils;

import java.util.Optional;

public class DisplayNameFilter implements JobClientFilter {

    @Override
    public void onCreating(AbstractJob job) {
        JobDetails jobDetails = job.getJobDetails();
        Optional<String> jobNameFromAnnotation = getJobNameFromAnnotation(jobDetails);
        if (jobNameFromAnnotation.isPresent()) {
            job.setJobName(getNameWithResolvedParameters(jobNameFromAnnotation.get(), jobDetails));
        } else {
            job.setJobName(JobUtils.getReadableNameFromJobDetails(jobDetails));
        }
    }

    private Optional<String> getJobNameFromAnnotation(JobDetails jobDetails) {
        Optional<org.jobrunr.jobs.annotations.Job> jobAnnotation = JobUtils.getJobAnnotation(jobDetails);
        return jobAnnotation
                .map(Job::name)
                .filter(StringUtils::isNotNullOrEmpty);
    }

    private String getNameWithResolvedParameters(String name, JobDetails jobDetails) {
        String finalName = name;
        for (int i = 0; i < jobDetails.getJobParameters().size(); i++) {
            finalName = finalName.replace("%" + i, jobDetails.getJobParameterValues()[i].toString());
        }
        return finalName;
    }
}
