package org.jobrunr.scheduling;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.PropertySource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

public class AsyncJobWithWrongReturnTypeTest {

    @Test
    void classAnnotatedWithAsyncJobAnnotationThrowsExceptionIfMethodReturnsSomething() {
        try (var context = ApplicationContext.run(PropertySource.of("test", Map.of("test.asyncjob.wrongtype.enabled", "true")))) {
            assertThatCode(() -> {
                context.getBean(AsyncJobTestServiceWithWrongReturnType.class);
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("An @AsyncJob cannot have a return value. int testMethodAsAsyncJobWithSomeReturnType() is defined as an @AsyncJob but has a return value.");
        }
    }
}
