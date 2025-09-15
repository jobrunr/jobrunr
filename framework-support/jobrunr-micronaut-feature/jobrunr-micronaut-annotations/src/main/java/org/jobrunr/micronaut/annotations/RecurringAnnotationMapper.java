package org.jobrunr.micronaut.annotations;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.TypedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;
import org.jobrunr.jobs.annotations.Recurring;

import java.util.List;

public class RecurringAnnotationMapper implements TypedAnnotationMapper<Recurring> {

    @Override
    public Class<Recurring> annotationType() {
        return Recurring.class;
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Recurring> annotation, VisitorContext visitorContext) {
        visitorContext.info("Mapping annotation: " + annotation, null);
        AnnotationValue<Executable> executable = AnnotationValue.builder(Executable.class)
                .member("processOnStartup", true)
                .build();
        return List.of(AnnotationValue.builder(annotation)
                .stereotype(executable)
                .build()
        );
    }
}
