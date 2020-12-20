package org.jobrunr.jobs;

import org.jobrunr.utils.reflection.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

public class JobDetails {

    private String className;
    private String staticFieldName;
    private String methodName;
    private List<JobParameter> jobParameters;
    private List<JobParameter> jobBoundVariables;

    private JobDetails() {
        // used for deserialization
    }

    public JobDetails(String className, String staticFieldName, String methodName,
        List<JobParameter> jobParameters) {
        this(className, staticFieldName, methodName, jobParameters, Collections.emptyList());
    }

    public JobDetails(String className, String staticFieldName, String methodName,
        List<JobParameter> jobParameters, List<JobParameter> boundVariables) {
        this.className = className;
        this.staticFieldName = staticFieldName;
        this.methodName = methodName;
        this.jobParameters = new ArrayList<>(jobParameters);
        this.jobBoundVariables = new ArrayList<>(boundVariables);
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

    public Object[] getJobBoundVariablesValues() {
        return jobBoundVariables.stream()
            .map(JobParameter::getObject)
            .toArray();
    }
}
