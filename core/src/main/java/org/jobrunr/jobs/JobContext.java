package org.jobrunr.jobs;

import org.jobrunr.jobs.states.StateName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class JobContext {

    public static final JobContext Null = new JobContext(null);
    public static final String JOB_RUNR_LOG_KEY = "jobRunrLog";

    private final Job job;

    private JobContext() {
        this.job = null;
    }

    protected JobContext(Job job) {
        this.job = job;
    }

    public UUID getJobId() {
        return job.getId();
    }

    public String getJobName() {
        return job.getJobName();
    }

    public StateName getJobState() {
        return job.getState();
    }

    public Instant getCreatedAt() {
        return job.getCreatedAt();
    }

    public Instant getUpdatedAt() {
        return job.getUpdatedAt();
    }

    public String getJobIdentifier() {
        return job.getJobSignature();
    }

    public void dashboardConsolePrintln(String toLog) {
        getLogs().add(new LogLine(toLog));
    }

    public Map<String, Object> getMetadata() {
        return job.getMetadata();
    }

    // marker interface for Jackson Serialization
    public interface Metadata {

    }

    private String logKey() {
        return JOB_RUNR_LOG_KEY + "-" + job.getJobStates().size();
    }

    private List<LogLine> getLogs() {
        if (!getMetadata().containsKey(logKey())) {
            getMetadata().put(logKey(), new ArrayList<LogLine>());
        }
        return cast(getMetadata().get(logKey()));
    }

    private static class LogLine implements Metadata {
        private Instant logInstant;
        private String logMessage;

        private LogLine() {
            // for json deserialization
        }

        public LogLine(String logMessage) {
            this.logInstant = Instant.now();
            this.logMessage = logMessage;
        }

        public Instant getLogInstant() {
            return logInstant;
        }

        public String getLogMessage() {
            return logMessage;
        }
    }
}
