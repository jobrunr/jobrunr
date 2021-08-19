package org.jobrunr.jobs;

import org.jobrunr.utils.reflection.ReflectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

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

    public JobDetails(String className, String staticFieldName, String methodName, List<JobParameter> jobParameters) {
        this.className = className;
        this.staticFieldName = staticFieldName;
        this.methodName = methodName;
        this.jobParameters = jobParameters != null ? new ArrayList<>(jobParameters) : new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public Optional<String> getStaticFieldName() {
        return Optional.ofNullable(staticFieldName);
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
