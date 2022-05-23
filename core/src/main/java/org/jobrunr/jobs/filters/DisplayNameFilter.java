package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.utils.JobUtils;
import org.jobrunr.utils.StringUtils;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisplayNameFilter implements JobClientFilter {

    private static final Pattern regexPattern = Pattern.compile("%X\\{(.*)\\}");

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
        String jobName = replaceJobParametersInDisplayName(name, jobDetails);
        String result = replaceMDCVariablesInDisplayName(jobName);
        return result;
    }

    private String replaceJobParametersInDisplayName(String name, JobDetails jobDetails) {
        String finalName = name;
        for (int i = 0; i < jobDetails.getJobParameters().size(); i++) {
            if(jobDetails.getJobParameterValues()[i] != null) {
                finalName = finalName.replace("%" + i, jobDetails.getJobParameterValues()[i].toString());
            }
        }
        return finalName;
    }

    private String replaceMDCVariablesInDisplayName(String name) {
        Matcher matcher = regexPattern.matcher(name);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = MDC.get(matcher.group(1));
            matcher.appendReplacement(result, replacement != null ? replacement : "(" + matcher.group(1) + " is not found in MDC)");
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
