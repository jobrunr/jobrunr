package org.jobrunr.jobs.context;

import org.jobrunr.jobs.Job;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class JobDashboardLogger {

    public enum Level {
        INFO, WARN, ERROR
    }

    public static final String JOBRUNR_LOG_KEY = "jobRunrDashboardLog";

    private final JobDashboardLogLines logLines;

    public JobDashboardLogger(Job job) {
        this.logLines = initLogLines(job);
    }

    public void info(String infoMessage) {
        logLines.add(new JobDashboardLogLine(Level.INFO, infoMessage));
    }

    public void warn(String warnMessage) {
        logLines.add(new JobDashboardLogLine(Level.WARN, warnMessage));
    }

    public void error(String errorMessage) {
        logLines.add(new JobDashboardLogLine(Level.ERROR, errorMessage));
    }

    private JobDashboardLogLines initLogLines(Job job) {
        Map<String, Object> jobMetadata = job.getMetadata();
        String logKey = logKey(job.getJobStates().size());
        jobMetadata.putIfAbsent(logKey, new JobDashboardLogLines());
        return cast(jobMetadata.get(logKey));
    }

    /**
     * Returns a unique key based on the current jobState (so that all logs regarding the first processing attempt can be displayed under the first processing view in the dashboard, ... )
     *
     * @param jobStateNbr the current state nbr - typically enqueued=1, processing=2, failed=3, scheduled=4, enqueued=5, processing=6, ...
     * @return a log key for the metadata matching the current job state.
     */
    private static String logKey(int jobStateNbr) {
        return JOBRUNR_LOG_KEY + "-" + jobStateNbr;
    }

    public static class JobDashboardLogLines implements JobContext.Metadata {

        /* Must be data structure that can be serialized to json and that allows iteration while being updated */
        private ConcurrentLinkedQueue<JobDashboardLogLine> logLines;

        public JobDashboardLogLines() {
            this.logLines = new ConcurrentLinkedQueue<>();
        }

        public JobDashboardLogLines(ConcurrentLinkedQueue<JobDashboardLogLine> logLines) {
            this.logLines = logLines;
        }

        public void add(JobDashboardLogLine line) {
            logLines.add(line);
        }

        public Queue<JobDashboardLogLine> getLogLines() {
            return logLines;
        }
    }

    public static class JobDashboardLogLine {

        private Level level;
        private Instant logInstant;
        private String logMessage;

        protected JobDashboardLogLine() {
            // for json deserialization
        }

        public JobDashboardLogLine(Level level, String logMessage) {
            this.level = level;
            this.logInstant = Instant.now();
            this.logMessage = logMessage;
        }

        public JobDashboardLogLine(Level level, Instant logInstant, String logMessage) {
            this.level = level;
            this.logInstant = logInstant;
            this.logMessage = logMessage;
        }

        public Level getLevel() {
            return level;
        }

        public Instant getLogInstant() {
            return logInstant;
        }

        public String getLogMessage() {
            return logMessage;
        }
    }
}
