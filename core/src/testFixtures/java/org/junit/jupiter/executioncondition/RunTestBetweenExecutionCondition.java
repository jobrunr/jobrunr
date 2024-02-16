package org.junit.jupiter.executioncondition;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.AnnotatedElement;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class RunTestBetweenExecutionCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<RunTestBetween> runTestBetweenOptional = findAnnotation(element, RunTestBetween.class);
        if (runTestBetweenOptional.isPresent()) {
            final RunTestBetween runTestBetween = runTestBetweenOptional.get();
            final LocalTime fromTime = LocalTime.parse(runTestBetween.from());
            final LocalTime toTime = LocalTime.parse(runTestBetween.to());

            if (LocalTime.now().isAfter(fromTime) && LocalTime.now().isBefore(toTime)) {
                return ConditionEvaluationResult.enabled(String.format("Test enabled as it is now (%s) between %s and %s", LocalTime.now(), fromTime, toTime));
            }
            return ConditionEvaluationResult.disabled(String.format("Test disabled as it is now (%s) not between %s and %s", LocalTime.now(), fromTime, toTime));

        }
        return ConditionEvaluationResult.enabled("@RunTestBetween is not present.");
    }
}
