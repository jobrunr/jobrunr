package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.utils.CollectionUtils;
import org.jobrunr.utils.JobUtils;
import org.jobrunr.utils.StringUtils;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.jobrunr.utils.JobUtils.getReadableNameFromJobDetails;

public class DefaultJobFilter implements JobClientFilter {

    private static final Pattern regexPattern = Pattern.compile("%X\\{(.*)\\}");

    @Override
    public void onCreating(AbstractJob job) {
        JobDetails jobDetails = job.getJobDetails();
        Optional<Job> jobAnnotation = getJobAnnotation(jobDetails);
        setJobName(job, jobAnnotation);
        setAmountOfRetries(job, jobAnnotation);
        setLabels(job, jobAnnotation);
    }

    @Override
    public void onCreated(AbstractJob job) {
        // nothing to do
    }

    private void setJobName(AbstractJob job, Optional<Job> jobAnnotation) {
        Optional<String> jobNameFromAnnotation = getFromAnnotation(jobAnnotation, Job::name);
        if (jobNameFromAnnotation.isPresent()) {
            job.setJobName(resolveParameters(jobNameFromAnnotation.get(), job));
        } else if (job.getJobName() == null) {
            job.setJobName(getReadableNameFromJobDetails(job.getJobDetails()));
        }
    }

    private void setAmountOfRetries(AbstractJob job, Optional<Job> jobAnnotation) {
        getIntegerFromAnnotation(jobAnnotation, Job::retries)
                .ifPresent(job::setAmountOfRetries);
    }

    private void setLabels(AbstractJob job, Optional<Job> jobAnnotation) {
        Optional<String[]> labelsFromAnnotation = getStringArrayFromAnnotation(jobAnnotation, Job::labels);
        if (labelsFromAnnotation.isPresent()) {
            job.setLabels(stream(labelsFromAnnotation.get()).map(s -> resolveParameters(s, job)).collect(toSet()));
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

    private Optional<String[]> getStringArrayFromAnnotation(Optional<Job> jobAnnotation, Function<Job, String[]> mappingFunction) {
        return jobAnnotation
                .map(mappingFunction)
                .filter(CollectionUtils::isNotNullOrEmpty);
    }

    private String resolveParameters(String name, AbstractJob abstractJob) {
        String jobName = replaceJobParameters(name, abstractJob.getJobDetails());
        return replaceMDCVariables(jobName);
    }

    private String replaceJobParameters(String name, JobDetails jobDetails) {
        String finalName = name;
        for (int i = 0; i < jobDetails.getJobParameters().size(); i++) {
            if (jobDetails.getJobParameterValues()[i] != null) {
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

    private Optional<Job> getJobAnnotation(JobDetails jobDetails) {
        return JobUtils.getJobAnnotation(jobDetails);
    }
}
