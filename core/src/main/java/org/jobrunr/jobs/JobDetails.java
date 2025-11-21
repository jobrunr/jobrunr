package org.jobrunr.jobs;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.utils.annotations.UsedForSerialization;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.jobrunr.utils.CollectionUtils.asArrayList;
import static org.jobrunr.utils.JobUtils.assertJobExists;
import static org.jobrunr.utils.ObjectUtils.ensureNonNull;

public class JobDetails {

    private String className;
    private final @Nullable String staticFieldName;
    private String methodName;
    private ArrayList<JobParameter> jobParameters;
    private @Nullable Boolean cacheable;

    @UsedForSerialization
    private JobDetails() {
        this.staticFieldName = null;
    }

    public JobDetails(JobRequest jobRequest) {
        this(jobRequest.getJobRequestHandler().getName(), null, "run", singletonList(new JobParameter(jobRequest)));
        assertJobExists(this);
        this.cacheable = true;
    }

    public JobDetails(String className, @Nullable String staticFieldName, String methodName, List<JobParameter> jobParameters) {
        this.className = className;
        this.staticFieldName = staticFieldName;
        this.methodName = methodName;
        this.jobParameters = asArrayList(jobParameters);
        this.cacheable = false;
    }

    public String getClassName() {
        return className;
    }

    public @Nullable String getStaticFieldName() {
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

    public @Nullable Boolean getCacheable() {
        return cacheable;
    }

    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
    }

    private String getJobParameterDeserializableClassName(JobParameter jobParameter) {
        return jobParameter.isNotDeserializable() ? ensureNonNull(jobParameter.getException()).getClass().getName() : jobParameter.getClassName();
    }
}
