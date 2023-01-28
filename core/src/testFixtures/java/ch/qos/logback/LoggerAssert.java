package ch.qos.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.Map;

import static ch.qos.logback.classic.Level.*;
import static java.util.Collections.emptyMap;

public class LoggerAssert extends AbstractAssert<LoggerAssert, ListAppender<ILoggingEvent>> {

    Condition<ILoggingEvent> debugLogs = new Condition<>(e -> e.getLevel().equals(DEBUG), "Debug logs");
    Condition<ILoggingEvent> infoLogs = new Condition<>(e -> e.getLevel().equals(INFO), "Info logs");
    Condition<ILoggingEvent> warningLogs = new Condition<>(e -> e.getLevel().equals(Level.WARN), "Warning logs");
    Condition<ILoggingEvent> errorLogs = new Condition<>(e -> e.getLevel().equals(Level.ERROR), "Error logs");
    Condition<ILoggingEvent> errorLogWithMessage = new Condition<>(e -> e.getLevel().equals(Level.ERROR), "Error logs");

    private LoggerAssert(ListAppender<ILoggingEvent> listAppender) {
        super(listAppender, LoggerAssert.class);
    }

    public static ListAppender<ILoggingEvent> initFor(Object object) {
        final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        final Logger logger = Whitebox.getInternalState(object, "LOGGER");
        logger.setLevel(DEBUG);
        logger.addAppender(listAppender);
        return listAppender;
    }

    public static LoggerAssert assertThat(ListAppender<ILoggingEvent> logger) {
        return new LoggerAssert(logger);
    }

    public LoggerAssert hasNoLogMessages() {
        Assertions.assertThat(actual.list).isEmpty();
        return this;
    }

    public LoggerAssert hasNoDebugLogMessages() {
        Assertions.assertThat(actual.list).doNotHave(logsWithLevel(DEBUG));
        return this;
    }

    public LoggerAssert hasNoInfoLogMessages() {
        Assertions.assertThat(actual.list).doNotHave(logsWithLevel(INFO));
        return this;
    }

    public LoggerAssert hasNoWarnLogMessages() {
        Assertions.assertThat(actual.list).doNotHave(logsWithLevel(WARN));
        return this;
    }

    public LoggerAssert hasNoErrorLogMessages() {
        Assertions.assertThat(actual.list).doNotHave(logsWithLevel(ERROR));
        return this;
    }

    public LoggerAssert hasErrorLogMessage(Class<?> clazz, String message) {
        Assertions.assertThat(actual.list)
                .have(errorLogs)
                .anyMatch(e -> e.getLevel().equals(Level.ERROR)
                        && e.getLoggerName().equals(clazz.getName())
                        && e.getFormattedMessage().equals(message));
        return this;
    }

    public LoggerAssert hasErrorMessage(String message) {
        Assertions.assertThat(actual.list).areAtLeastOne(logsWithLevelAndMessage(ERROR, message));
        return this;
    }

    public LoggerAssert hasNoErrorMessageContaining(String message) {
        Assertions.assertThat(actual.list).areNot(logsWithLevelAndMessage(ERROR, message));
        return this;
    }

    public LoggerAssert hasInfoMessage(String message) {
        Assertions.assertThat(actual.list).areAtLeastOne(logsWithLevelAndMessage(INFO, message));
        return this;
    }

    public LoggerAssert hasInfoMessageContaining(String message) {
        Assertions.assertThat(actual.list).areAtLeastOne(logsWithLevelAndMessageContaining(INFO, message));
        return this;
    }

    public LoggerAssert hasInfoMessageContaining(String message, int times) {
        Assertions.assertThat(actual.list).areExactly(times, logsWithLevelAndMessageContaining(INFO, message));
        return this;
    }

    public LoggerAssert hasDebugMessageContaining(String message) {
        return hasDebugMessageContaining(message, 1, emptyMap());
    }

    public LoggerAssert hasDebugMessageContaining(String message, Map<String, String> mdcData) {
        return hasDebugMessageContaining(message, 1, mdcData);
    }

    public LoggerAssert hasDebugMessageContaining(String message, int times, Map<String, String> mdcData) {
        Assertions.assertThat(actual.list).areExactly(times, logsWithLevelAndMessageContaining(DEBUG, message, mdcData));
        return this;
    }

    public LoggerAssert hasWarningMessageContaining(String message) {
        return hasWarningMessageContaining(message, 1, emptyMap());
    }

    public LoggerAssert hasWarningMessageContaining(String message, Map<String, String> mdcData) {
        return hasWarningMessageContaining(message, 1, mdcData);
    }

    public LoggerAssert hasWarningMessageContaining(String message, int times, Map<String, String> mdcData) {
        Assertions.assertThat(actual.list).areExactly(times, logsWithLevelAndMessageContaining(WARN, message, mdcData));
        return this;
    }

    public LoggerAssert hasLogEventWithMDCData(int logEvent, String mdcKey, String mdcValue) {
        Assertions.assertThat(actual.list.get(logEvent).getMDCPropertyMap()).containsEntry(mdcKey, mdcValue);
        return this;
    }

    private Condition<ILoggingEvent> logsWithLevel(Level level) {
        return new Condition<ILoggingEvent>(e -> e.getLevel().equals(level), level + " logs");
    }

    private Condition<ILoggingEvent> logsWithLevelAndMessage(Level level, String message) {
        return new Condition<ILoggingEvent>(e -> e.getLevel().equals(level) && e.toString().replace("[" + level + "] ", "").equals(message), level + " logs with message: " + message);
    }

    private Condition<ILoggingEvent> logsWithLevelAndMessageContaining(Level level, String message) {
        return new Condition<ILoggingEvent>(e -> e.getLevel().equals(level) && e.toString().replace("[" + level + "] ", "").contains(message), level + " logs with message: " + message);
    }

    private Condition<ILoggingEvent> logsWithLevelAndMessageContaining(Level level, String message, Map<String, String> mdcData) {
        return new Condition<ILoggingEvent>(e -> matchesLoggingEvent(e, level, message, mdcData), level + " logs with message: " + message);
    }

    private boolean matchesLoggingEvent(ILoggingEvent event, Level level, String message, Map<String, String> mdcData) {
        if(event.getLevel().equals(level) && event.toString().replace("[" + level + "] ", "").contains(message)) {
            for (String key : mdcData.keySet()) {
                if(!event.getMDCPropertyMap().containsKey(key) || !event.getMDCPropertyMap().get(key).equals(mdcData.get(key))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
