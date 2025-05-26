package org.jobrunr.jobs;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.jobrunr.utils.CollectionUtils.asArrayList;
import static org.jobrunr.utils.JobUtils.assertJobExists;

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
        this(jobRequest.getJobRequestHandler().getName(), null, "run", singletonList(new JobParameter(jobRequest)));
        assertJobExists(this);
        this.cacheable = true;
    }

    public JobDetails(String className, String staticFieldName, String methodName, List<JobParameter> jobParameters) {
        this.className = className;
        this.staticFieldName = staticFieldName;
        this.methodName = methodName;
        this.jobParameters = asArrayList(jobParameters);
        this.cacheable = false;
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
                .map(this::getJobParameterDeserializableClassName)
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

    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
    }

    private String getJobParameterDeserializableClassName(JobParameter jobParameter) {
        return jobParameter.isNotDeserializable() ? jobParameter.getException().getClass().getName() : jobParameter.getClassName();
    }
}
