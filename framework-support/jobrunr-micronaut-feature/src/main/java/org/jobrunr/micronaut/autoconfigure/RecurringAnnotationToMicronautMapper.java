package org.jobrunr.micronaut.autoconfigure;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.TypedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;
import org.jobrunr.jobs.annotations.Recurring;

import java.util.List;

public class RecurringAnnotationToMicronautMapper implements TypedAnnotationMapper<Recurring> {

    @Override
    public Class<Recurring> annotationType() {
        return Recurring.class;
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Recurring> annotation, VisitorContext visitorContext) {
        return List.of(AnnotationValue.builder(Executable.class)
                .member("processOnStartup", true)
                .build());
    }
}
