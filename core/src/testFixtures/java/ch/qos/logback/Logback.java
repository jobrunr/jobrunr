package ch.qos.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

public class Logback {

    public static TemporarilyLogLevelChange temporarilyChangeLogLevel(Class clazz, Level level) {
        return new TemporarilyLogLevelChange(clazz.getName(), level);
    }

    public static void changeLogLevel(Class clazz, Level level) {
        changeLogLevel(clazz.getName(), level);
    }

    public static void changeLogLevel(String loggerName, Level level) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(loggerName);
        if (logger != null) {
            logger.setLevel(level);
        } else {
            throw new UnsupportedOperationException("Could not find 'root' logger");
        }
    }

    public static Level getLogLevel(String loggerName) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(loggerName);
        if (logger != null) {
            return logger.getLevel();
        } else {
            throw new UnsupportedOperationException("Could not find 'root' logger");
        }
    }

    public static class TemporarilyLogLevelChange implements AutoCloseable {

        private final String loggerName;
        private final Level originalLevel;

        public TemporarilyLogLevelChange(String loggerName, Level tempLevel) {
            this.loggerName = loggerName;
            this.originalLevel = getLogLevel(loggerName);
            changeLogLevel(loggerName, tempLevel);
        }

        @Override
        public void close() {
            changeLogLevel(loggerName, originalLevel);
        }
    }
}
