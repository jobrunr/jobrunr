package ch.qos.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.mockito.internal.util.reflection.Whitebox;

public class LoggerAssert extends AbstractAssert<LoggerAssert, ListAppender<ILoggingEvent>> {

    Condition<ILoggingEvent> debugLogs = new Condition<>(e -> e.getLevel().equals(Level.DEBUG), "Debug logs");
    Condition<ILoggingEvent> infoLogs = new Condition<>(e -> e.getLevel().equals(Level.INFO), "Info logs");
    Condition<ILoggingEvent> warningLogs = new Condition<>(e -> e.getLevel().equals(Level.WARN), "Warning logs");

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
        Assertions.assertThat(actual.list).doNotHave(debugLogs);
        return this;
    }

    public LoggerAssert hasNoInfoLogMessages() {
        Assertions.assertThat(actual.list).doNotHave(infoLogs);
        return this;
    }

    public LoggerAssert hasNoWarnLogMessages() {
        Assertions.assertThat(actual.list).doNotHave(warningLogs);
        return this;
    }
}
