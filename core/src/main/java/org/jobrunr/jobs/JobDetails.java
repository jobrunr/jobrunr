package org.jobrunr.jobs;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jobrunr.utils.CollectionUtils.asArrayList;

public class JobDetails {

    private final String className;
    private final String staticFieldName;
    private final String methodName;
    private final ArrayList<JobParameter> jobParameters;
    private Boolean cacheable;

    private JobDetails() {
        this(null, null, null, null);
        // used for deserialization
    }

    public JobDetails(JobRequest jobRequest) {
        this(jobRequest.getJobRequestHandler().getName(), null, "run", asList(new JobParameter(jobRequest)));
        this.cacheable = true;
    }

    public JobDetails(String className, String staticFieldName, String methodName, List<JobParameter> jobParameters) {
        this.className = className;
        this.staticFieldName = staticFieldName;
        this.methodName = methodName;
        this.jobParameters = asArrayList(jobParameters);
    }

    public String getClassName() {
        return className;
    }

    public String getStaticFieldName() {
        return staticFieldName;
    }

    public boolean hasStaticFieldName() {
        return staticFieldName != null;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<JobParameter> getJobParameters() {
        return unmodifiableList(jobParameters);
    }

    public Class[] getJobParameterTypes() {
        return jobParameters.stream()
                .map(JobParameter::getClassName)
                .map(ReflectionUtils::toClass)
                .toArray(Class[]::new);
    }

    public Object[] getJobParameterValues() {
        return jobParameters.stream()
                .map(JobParameter::getObject)
                .toArray();
    }

    public Boolean getCacheable() {
        return cacheable;
    }

    public void setCacheable(Boolean cacheable) {
        this.cacheable = cacheable;
    }
}
