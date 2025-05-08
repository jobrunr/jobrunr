package org.jobrunr.jobs.context;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobDashboardLogger.Level;
import org.jobrunr.server.runner.RunnerJobContext;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.Iterator;

public class JobRunrDashboardLogger implements Logger {

    private static final ThreadLocal<JobDashboardLogger> jobDashboardLoggerThreadLocal = new ThreadLocal<>();
    private final Logger logger;
    private final Level threshold;

    public JobRunrDashboardLogger(Logger logger) {
        this(logger, Level.INFO);
    }

    public JobRunrDashboardLogger(Logger logger, Level threshold) {
        this.logger = logger;
        this.threshold = threshold;
    }

    public static void setJob(Job job) {
        jobDashboardLoggerThreadLocal.set(new RunnerJobContext(job).logger());
    }

    public static void clearJob() {
        jobDashboardLoggerThreadLocal.remove();
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        logger.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logger.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        logger.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logger.trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        logger.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        logger.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        logger.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        logger.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        logger.trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logger.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        logger.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        logger.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        logger.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        logger.debug(marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        logger.debug(marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
        logInfoToJobDashboard(msg);
    }

    @Override
    public void info(String format, Object arg) {
        logger.info(format, arg);
        logInfoToJobDashboard(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logger.info(format, arg1, arg2);
        logInfoToJobDashboard(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        logger.info(format, arguments);
        logInfoToJobDashboard(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info(msg, t);
        logInfoToJobDashboard(msg);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        logger.info(marker, msg);
        logInfoToJobDashboard(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        logger.info(marker, format, arg);
        logInfoToJobDashboard(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        logger.info(marker, format, arg1, arg2);
        logInfoToJobDashboard(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        logger.info(marker, format, arguments);
        logInfoToJobDashboard(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        logger.info(marker, msg, t);
        logInfoToJobDashboard(marker, msg);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
        logWarnToJobDashboard(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logger.warn(format, arg);
        logWarnToJobDashboard(format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(format, arg1, arg2);
        logWarnToJobDashboard(format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn(format, arguments);
        logWarnToJobDashboard(format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
        logWarnToJobDashboard(msg);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        logger.warn(marker, msg);
        logWarnToJobDashboard(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        logger.warn(marker, format, arg);
        logWarnToJobDashboard(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        logger.warn(marker, format, arg1, arg2);
        logWarnToJobDashboard(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        logger.warn(marker, format, arguments);
        logWarnToJobDashboard(marker, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        logger.warn(marker, msg, t);
        logWarnToJobDashboard(marker, msg);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
        logErrorToJobDashboard(msg);
    }

    @Override
    public void error(String format, Object arg) {
        logger.error(format, arg);
        logErrorToJobDashboard(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logger.error(format, arg1, arg2);
        logErrorToJobDashboard(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        logger.error(format, arguments);
        logErrorToJobDashboard(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(msg, t);
        logErrorToJobDashboard(msg);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        logger.error(marker, msg);
        logErrorToJobDashboard(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        logger.error(marker, format, arg);
        logErrorToJobDashboard(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        logger.error(marker, format, arg1, arg2);
        logErrorToJobDashboard(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        logger.error(marker, format, arguments);
        logErrorToJobDashboard(marker, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        logger.error(marker, msg, t);
        logErrorToJobDashboard(marker, msg);
    }

    private void logInfoToJobDashboard(String message) {
        logInfoToJobDashboard(null, message);
    }

    private void logInfoToJobDashboard(Marker marker, String message) {
        if (threshold.compareTo(Level.INFO) > 0) return;
        if (jobDashboardLoggerThreadLocal.get() != null) {
            jobDashboardLoggerThreadLocal.get().info(formatMarker(marker) + message);
        }
    }

    private void logInfoToJobDashboard(String format, Object... args) {
        logInfoToJobDashboard(null, format, args);
    }

    private void logInfoToJobDashboard(Marker marker, String format, Object... args) {
        if (threshold.compareTo(Level.INFO) > 0) return;
        if (jobDashboardLoggerThreadLocal.get() != null) {
            FormattingTuple tp = MessageFormatter.arrayFormat(format, args);
            jobDashboardLoggerThreadLocal.get().info(formatMarker(marker) + tp.getMessage());
        }
    }

    private void logWarnToJobDashboard(String message) {
        logWarnToJobDashboard(null, message);
    }

    private void logWarnToJobDashboard(Marker marker, String message) {
        if (threshold.compareTo(Level.WARN) > 0) return;
        if (jobDashboardLoggerThreadLocal.get() != null) {
            jobDashboardLoggerThreadLocal.get().warn(formatMarker(marker) + message);
        }
    }

    private void logWarnToJobDashboard(String format, Object... args) {
        logWarnToJobDashboard(null, format, args);
    }

    private void logWarnToJobDashboard(Marker marker, String format, Object... args) {
        if (threshold.compareTo(Level.WARN) > 0) return;
        if (jobDashboardLoggerThreadLocal.get() != null) {
            FormattingTuple tp = MessageFormatter.arrayFormat(format, args);
            jobDashboardLoggerThreadLocal.get().warn(formatMarker(marker) + tp.getMessage());
        }
    }

    private void logErrorToJobDashboard(String message) {
        logErrorToJobDashboard(null, message);
    }

    private void logErrorToJobDashboard(Marker marker, String message) {
        if (jobDashboardLoggerThreadLocal.get() != null) {
            jobDashboardLoggerThreadLocal.get().error(formatMarker(marker) + message);
        }
    }

    private void logErrorToJobDashboard(String format, Object... args) {
        logErrorToJobDashboard(null, format, args);
    }

    private void logErrorToJobDashboard(Marker marker, String format, Object... args) {
        if (jobDashboardLoggerThreadLocal.get() != null) {
            FormattingTuple tp = MessageFormatter.arrayFormat(format, args);
            jobDashboardLoggerThreadLocal.get().error(formatMarker(marker) + tp.getMessage());
        }
    }

    private String formatMarker(Marker marker) {
        if (marker == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(marker.getName());
        if (marker.hasReferences()) {
            Iterator<Marker> iterator = marker.iterator();
            sb.append(" (");
            while (iterator.hasNext()) {
                sb.append(iterator.next().getName());
                if (iterator.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(")");
        }
        sb.append("] ");
        return sb.toString();
    }
}
