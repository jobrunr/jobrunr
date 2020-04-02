package org.jobrunr.jobs;

import org.jobrunr.utils.reflection.ReflectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

public class JobDetails {

    private String lambdaType;
    private String className;
    private String staticFieldName;
    private String methodName;
    private ArrayList<JobParameter> jobParameters;

    private JobDetails() {
        // used for deserialization
    }

    public JobDetails(String lambdaType, String className, String staticFieldName, String methodName, List<JobParameter> jobParameters) {
        this.lambdaType = lambdaType;
        this.className = className;
        this.staticFieldName = staticFieldName;
        this.methodName = methodName;
        this.jobParameters = new ArrayList<>(jobParameters);
    }

    public String getLambdaType() {
        return lambdaType;
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
}
