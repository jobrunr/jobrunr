package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jobrunr.jobs.JobParameter;

import java.util.ArrayList;

@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.ANY)
public abstract class JobDetailsMixin {
    @JsonCreator
    public JobDetailsMixin(
            @JsonProperty("className") String className,
            @JsonProperty("staticFieldName") String staticFieldName,
            @JsonProperty("methodName") String methodName,
            @JsonProperty("jobParameters") ArrayList<JobParameter> jobParameters
    ) {}
}
