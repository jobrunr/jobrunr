package org.jobrunr.jobs.context;

import org.assertj.core.api.Condition;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobDashboardLogger.JobDashboardLogLines;
import org.jobrunr.jobs.context.JobDashboardLogger.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.Map;

import static org.assertj.core.api.Assertions.not;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobRunrDashboardLoggerTest {

    @Mock
    private Logger slfLogger;
    private JobRunrDashboardLogger jobRunrDashboardLogger;

    @BeforeEach
    void setUpJobLogger() {
        jobRunrDashboardLogger = new JobRunrDashboardLogger(slfLogger);
    }

    @AfterEach
    void cleanUp() {
        JobRunrDashboardLogger.clearJob();
    }

    @Test
    void testInfoLoggingWithoutJob() {
        jobRunrDashboardLogger.info("simple message");

        verify(slfLogger).info("simple message");
    }

    @Test
    void testInfoLoggingWithJob() {
        final Job job = aJobInProgress().build();
        JobRunrDashboardLogger.setJob(job);

        jobRunrDashboardLogger.info("simple message");

        verify(slfLogger).info("simple message");
        assertThat(job).hasMetadata(InfoLog.withMessage("simple message"));
    }

    @Test
    void testInfoLoggingWithJobAndFormattingOneArgument() {
        final Job job = aJobInProgress().build();
        JobRunrDashboardLogger.setJob(job);

        jobRunrDashboardLogger.info("simple message {}", "hello");

        verify(slfLogger).info("simple message {}", "hello");
        assertThat(job).hasMetadata(InfoLog.withMessage("simple message hello"));
    }

    @Test
    void testInfoLoggingWithJobAndFormattingMultipleArguments() {
        final Job job = aJobInProgress().build();
        JobRunrDashboardLogger.setJob(job);

        jobRunrDashboardLogger.info("simple message {} {} {}", "hello", "again", "there");

        verify(slfLogger).info("simple message {} {} {}", "hello", "again", "there");
        assertThat(job).hasMetadata(InfoLog.withMessage("simple message hello again there"));
    }

    @Test
    void testWarnLoggingWithoutJob() {
        jobRunrDashboardLogger.warn("simple message");

        verify(slfLogger).warn("simple message");
    }

    @Test
    void testWarnLoggingWithJob() {
        final Job job = aJobInProgress().build();
        JobRunrDashboardLogger.setJob(job);

        jobRunrDashboardLogger.warn("simple message");

        verify(slfLogger).warn("simple message");
        assertThat(job).hasMetadata(WarnLog.withMessage("simple message"));
    }

    @Test
    void testWarnLoggingWithJobAndFormattingOneArgument() {
        final Job job = aJobInProgress().build();
        JobRunrDashboardLogger.setJob(job);

        jobRunrDashboardLogger.warn("simple message {}", "hello");

        verify(slfLogger).warn("simple message {}", "hello");
        assertThat(job).hasMetadata(WarnLog.withMessage("simple message hello"));
    }

    @Test
    void testWarnLoggingWithJobAndFormattingMultipleArguments() {
        final Job job = aJobInProgress().build();
        JobRunrDashboardLogger.setJob(job);

        jobRunrDashboardLogger.warn("simple message {} {} {}", "hello", "again", "there");

        verify(slfLogger).warn("simple message {} {} {}", "hello", "again", "there");
        assertThat(job).hasMetadata(WarnLog.withMessage("simple message hello again there"));
    }

    @Test
    void testErrorLoggingWithoutJob() {
        jobRunrDashboardLogger.error("simple message");

        verify(slfLogger).error("simple message");
    }

    @Test
    void testErrorLoggingWithJob() {
        final Job job = aJobInProgress().build();
        JobRunrDashboardLogger.setJob(job);

        jobRunrDashboardLogger.error("simple message");

        verify(slfLogger).error("simple message");
        assertThat(job).hasMetadata(ErrorLog.withMessage("simple message"));
    }

    @Test
    void testErrorLoggingWithJobAndFormattingOneArgument() {
        final Job job = aJobInProgress().build();
        JobRunrDashboardLogger.setJob(job);

        jobRunrDashboardLogger.error("simple message {}", "hello");

        verify(slfLogger).error("simple message {}", "hello");
        assertThat(job).hasMetadata(ErrorLog.withMessage("simple message hello"));
    }

    @Test
    void testErrorLoggingWithJobAndFormattingMultipleArguments() {
        final Job job = aJobInProgress().build();
        JobRunrDashboardLogger.setJob(job);

        jobRunrDashboardLogger.error("simple message {} {} {}", "hello", "again", "there");

        verify(slfLogger).error("simple message {} {} {}", "hello", "again", "there");
        assertThat(job).hasMetadata(ErrorLog.withMessage("simple message hello again there"));
    }

    @Test
    void testInfoLoggingWithJobAndThreshold() {
        jobRunrDashboardLogger = new JobRunrDashboardLogger(slfLogger, Level.WARN);
        final Job job = aJobInProgress().build();
        JobRunrDashboardLogger.setJob(job);

        jobRunrDashboardLogger.info("simple message");

        verify(slfLogger).info("simple message");
        assertThat(job).hasMetadata(not(InfoLog.withMessage("simple message")));
    }

    @Test
    void testWarnLoggingWithJobAndThreshold() {
        jobRunrDashboardLogger = new JobRunrDashboardLogger(slfLogger, Level.ERROR);
        final Job job = aJobInProgress().build();
        JobRunrDashboardLogger.setJob(job);

        jobRunrDashboardLogger.warn("simple message");

        verify(slfLogger).warn("simple message");
        assertThat(job).hasMetadata(not(WarnLog.withMessage("simple message")));
    }

    private static class InfoLog extends LogCondition {

        private InfoLog(String message) {
            super(Level.INFO, message);
        }

        public static InfoLog withMessage(String message) {
            return new InfoLog(message);
        }
    }

    private static class WarnLog extends LogCondition {

        private WarnLog(String message) {
            super(Level.WARN, message);
        }

        public static WarnLog withMessage(String message) {
            return new WarnLog(message);
        }
    }

    private static class ErrorLog extends LogCondition {

        private ErrorLog(String message) {
            super(Level.ERROR, message);
        }

        public static ErrorLog withMessage(String message) {
            return new ErrorLog(message);
        }
    }

    private static class LogCondition extends Condition {

        private final Level level;
        private final String message;

        protected LogCondition(Level level, String message) {
            this.level = level;
            this.message = message;
        }

        @Override
        public boolean matches(Object value) {
            Map<String, Object> metadata = cast(value);
            JobDashboardLogLines logLines = cast(metadata.get("jobRunrDashboardLog-2"));
            return logLines.getLogLines().stream().anyMatch(logLine -> level.equals(logLine.getLevel()) && message.equals(logLine.getLogMessage()));
        }
    }

}