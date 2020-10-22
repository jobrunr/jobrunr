package ch.qos.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.mockito.internal.util.reflection.Whitebox;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.WARN;

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

    public LoggerAssert hasErrorMessage(String message) {
        Assertions.assertThat(actual.list).areAtLeastOne(logsWithLevelAndMessage(ERROR, message));
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

    private Condition<ILoggingEvent> logsWithLevel(Level level) {
        return new Condition<ILoggingEvent>(e -> e.getLevel().equals(level), level + " logs");
    }

    private Condition<ILoggingEvent> logsWithLevelAndMessage(Level level, String message) {
        return new Condition<ILoggingEvent>(e -> e.getLevel().equals(level) && message.equals(e.getMessage()), level + " logs");
    }

    private Condition<ILoggingEvent> logsWithLevelAndMessageContaining(Level level, String message) {
        return new Condition<ILoggingEvent>(e -> e.getLevel().equals(level) && e.getMessage().contains(message), level + " logs");
    }
}
