package org.jobrunr.jobs.details;

import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class KotlinJobDetailsBuilder extends JobDetailsBuilder {

    public KotlinJobDetailsBuilder(JobRunrJob jobRunrJob, Object... params) {
        super(getLocalVariables(jobRunrJob, params));
    }

    private static List<Object> getLocalVariables(JobRunrJob jobRunrJob, Object... params) {
        List<Object> result = new ArrayList<>();
        result.add(jobRunrJob);
        result.addAll(asList(params));
        return result;
    }
}
