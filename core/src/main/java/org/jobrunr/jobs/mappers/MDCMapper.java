package org.jobrunr.jobs.mappers;

import org.jobrunr.jobs.Job;
import org.slf4j.MDC;

import java.util.Map;
import java.util.stream.Collectors;

public class MDCMapper {

    public static final String JOBRUNR_MDC_KEY = "mdc";
    public static final String JOBRUNR_MDC_JOB_ID_KEY = "jobrunr.jobId";
    public static final String JOBRUNR_MDC_JOB_NAME_KEY = "jobrunr.jobName";
    public static final String JOBRUNR_MDC_JOB_SIGNATURE_KEY = "jobrunr.jobSignature";

    private MDCMapper() {
        // private constructor for SonarQube
    }

    public static void saveMDCContextToJob(Job job) {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        if (mdcContext == null) return;
        mdcContext.forEach((key, value) -> {
            if (value != null) {
                job.getMetadata().put(JOBRUNR_MDC_KEY + "-" + key, value);
            }
        });
    }

    public static void loadMDCContextFromJob(Job job) {
        Map<String, Object> jobMetadata = job.getMetadata();
        Map<String, String> mdcContextMap = jobMetadata.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(JOBRUNR_MDC_KEY + "-"))
                .collect(Collectors.toMap(entry -> entry.getKey().substring(4), entry -> entry.getValue().toString()));
        mdcContextMap.put(JOBRUNR_MDC_JOB_ID_KEY, job.getId().toString());
        mdcContextMap.put(JOBRUNR_MDC_JOB_NAME_KEY, job.getJobName());
        mdcContextMap.put(JOBRUNR_MDC_JOB_SIGNATURE_KEY, job.getJobSignature());
        MDC.setContextMap(mdcContextMap);
    }

}
