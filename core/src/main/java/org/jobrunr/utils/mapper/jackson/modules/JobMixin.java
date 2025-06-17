package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jobrunr.jobs.states.JobState;

import java.util.concurrent.CopyOnWriteArrayList;

public class JobMixin {

    @JsonIgnoreProperties({"recurringJobId"})
    CopyOnWriteArrayList<JobState> jobHistory;
}
