package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.utils.StringUtils;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jobrunr.utils.JobUtils.getJobAnnotation;
import static org.jobrunr.utils.JobUtils.getReadableNameFromJobDetails;

public class DefaultJobFilter implements JobClientFilter {

    private static final Pattern regexPattern = Pattern.compile("%X\\{(.*)\\}");

    @Override
    public void onCreating(AbstractJob job) {
        JobDetails jobDetails = job.getJobDetails();
        Optional<Job> jobAnnotation = getJobAnnotation(jobDetails);
        setJobName(job, jobAnnotation);
        setAmountOfRetries(job, jobAnnotation);
    }

    private void setJobName(AbstractJob job, Optional<Job> jobAnnotation) {
        Optional<String> jobNameFromAnnotation = getFromAnnotation(jobAnnotation, Job::name);
        if (jobNameFromAnnotation.isPresent()) {
            job.setJobName(resolveParameters(jobNameFromAnnotation.get(), job));
        } else {
            job.setJobName(getReadableNameFromJobDetails(job.getJobDetails()));
        }
    }

    private void setAmountOfRetries(AbstractJob job, Optional<Job> jobAnnotation) {
        Optional<Integer> amountOfRetriesFromAnnotation = getIntegerFromAnnotation(jobAnnotation, Job::retries);
        if (amountOfRetriesFromAnnotation.isPresent()) {
            job.setAmountOfRetries(amountOfRetriesFromAnnotation.get());
        }
    }

    private Optional<String> getFromAnnotation(Optional<Job> jobAnnotation, Function<Job, String> mappingFunction) {
        return jobAnnotation
                .map(mappingFunction)
                .filter(StringUtils::isNotNullOrEmpty);
    }

    private Optional<Integer> getIntegerFromAnnotation(Optional<Job> jobAnnotation, Function<Job, Integer> mappingFunction) {
        return jobAnnotation
                .map(mappingFunction)
                .filter(val -> val > Job.NBR_OF_RETRIES_NOT_PROVIDED);
    }

    private String resolveParameters(String name, AbstractJob abstractJob) {
        String jobName = replaceJobParameters(name, abstractJob.getJobDetails());
        return replaceMDCVariables(jobName);
    }

    private String replaceJobParameters(String name, JobDetails jobDetails) {
        String finalName = name;
        for (int i = 0; i < jobDetails.getJobParameters().size(); i++) {
            if(jobDetails.getJobParameterValues()[i] != null) {
                finalName = finalName.replace("%" + i, jobDetails.getJobParameterValues()[i].toString());
            }
        }
        return finalName;
    }

    private String replaceMDCVariables(String name) {
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
