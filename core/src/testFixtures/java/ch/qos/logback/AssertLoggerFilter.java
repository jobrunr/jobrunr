package ch.qos.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.Map;

public class AssertLoggerFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (logEvent(event)) {
            return FilterReply.NEUTRAL;
        } else {
            event.prepareForDeferredProcessing();
            return FilterReply.DENY;
        }
    }

    private boolean logEvent(ILoggingEvent event) {
        Map<String, String> propertyMap = event.getLoggerContextVO().getPropertyMap();
        if (!propertyMap.containsKey("AssertLoggerOriginalLevel-" + event.getLoggerName())) return true;
        int originalLevel = Integer.parseInt(propertyMap.get("AssertLoggerOriginalLevel-" + event.getLoggerName()));
        return event.getLevel().toInt() >= originalLevel;
    }
}