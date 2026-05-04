package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.states.JobState;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.ANY)
public abstract class JobMixin {

    @JsonCreator
    public JobMixin(
            @JsonProperty("id") UUID id,
            @JsonProperty("version") int version,
            @JsonProperty("jobDetails") JobDetails jobDetails,
            @JsonProperty("jobHistory") CopyOnWriteArrayList<JobState> jobHistory,
            @JsonProperty("metadata") ConcurrentMap<String, Object> metadata
    ) {}

    @JsonIgnoreProperties({"recurringJobId"})
    CopyOnWriteArrayList<JobState> jobHistory;
}
