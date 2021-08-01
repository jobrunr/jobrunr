package org.jobrunr.jobs;

import org.jobrunr.utils.reflection.ReflectionUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static org.jobrunr.utils.CollectionUtils.asArrayList;

public class JobDetails {

    private String className;
    private String staticFieldName;
    private String methodName;
    private ArrayList<JobParameter> jobParameters;

    private JobDetails() {
        // used for deserialization
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
}
