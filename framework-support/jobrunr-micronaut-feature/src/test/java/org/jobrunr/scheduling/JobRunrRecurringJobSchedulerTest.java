package org.jobrunr.scheduling;

import io.micronaut.inject.ExecutableMethod;
import org.jobrunr.micronaut.annotations.Recurring;
import org.jobrunr.scheduling.cron.CronExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.util.Optional;

import static io.micronaut.core.reflect.ReflectionUtils.getRequiredMethod;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRunrRecurringJobSchedulerTest {

    @Mock
    JobScheduler jobScheduler;


    JobRunrRecurringJobScheduler jobRunrRecurringJobScheduler;

    @BeforeEach
    void setUpMicronautScheduler() {
        jobRunrRecurringJobScheduler = new JobRunrRecurringJobScheduler(jobScheduler);
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationWillAutomaticallyBeRegistered() {
        final ExecutableMethod executableMethod = mock(ExecutableMethod.class);
        final Method method = getRequiredMethod(MyServiceWithRecurringJob.class, "myRecurringMethod");
        when(executableMethod.getTargetMethod()).thenReturn(method);

        when(executableMethod.stringValue(Recurring.class, "id")).thenReturn(Optional.of("my-recurring-job"));
        when(executableMethod.stringValue(Recurring.class, "cron")).thenReturn(Optional.of("*/15 * * * *"));
        when(executableMethod.stringValue(Recurring.class, "zone")).thenReturn(Optional.empty());

        jobRunrRecurringJobScheduler.schedule(executableMethod);

        verify(jobScheduler).scheduleRecurrently(eq("my-recurring-job"), any(), eq(CronExpression.create("*/15 * * * *")), eq(ZoneId.systemDefault()));
    }

    @Test
    void beansWithUnsupportedMethodsAnnotatedWithRecurringAnnotationWillThrowException() {
        final ExecutableMethod executableMethod = mock(ExecutableMethod.class);
        final Method method = getRequiredMethod(MyUnsupportedService.class, "myRecurringMethod", String.class);
        when(executableMethod.getTargetMethod()).thenReturn(method);

        assertThatThrownBy(() -> jobRunrRecurringJobScheduler.schedule(executableMethod)).isInstanceOf(IllegalStateException.class);
    }

    public static class MyServiceWithRecurringJob {

        @Recurring(id = "my-recurring-job", cron = "*/15 * * * *")
        public void myRecurringMethod() {
            System.out.print("My recurring job method");
        }
    }

    public static class MyUnsupportedService {

        @Recurring(id = "my-recurring-job", cron = "*/15 * * * *")
        public void myRecurringMethod(String parameter) {
            System.out.print("My unsupported recurring job method because of method argument");
        }
    }
}
