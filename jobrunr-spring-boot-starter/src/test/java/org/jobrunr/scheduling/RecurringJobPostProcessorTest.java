package org.jobrunr.scheduling;

import org.jobrunr.annotations.Recurring;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.scheduling.cron.CronExpression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RecurringJobPostProcessorTest {

    @Mock
    private JobScheduler jobScheduler;

    @Test
    void beansWithoutMethodsAnnotatedWithRecurringAnnotationWillNotBeHandled() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithoutRecurringAnnotation(), "not important");

        // THEN
        verifyNoInteractions(jobScheduler);
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationWillAutomaticallyBeRegistered() {
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);

        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringJob(), "not important");

        verify(jobScheduler).scheduleRecurrently(eq("my-recurring-job"), any(JobDetails.class), eq(CronExpression.create("0 0/15 * * *")), any(ZoneId.class));
    }

    @Test
    void beansWithUnsupportedMethodsAnnotatedWithRecurringAnnotationWillThrowException() {
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);

        assertThatThrownBy(() -> recurringJobPostProcessor.postProcessAfterInitialization(new MyUnsupportedService(), "not important"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Methods annotated with " + Recurring.class.getName() + " can not have parameters.");
    }

    public static class MyServiceWithoutRecurringAnnotation {

        public void myMethodWithoutRecurringAnnotation() {
            System.out.print("My method without Recurring annotation");
        }
    }

    public static class MyServiceWithRecurringJob {

        @Recurring(id = "my-recurring-job", cron = "0 0/15 * * *")
        public void myRecurringMethod() {
            System.out.print("My recurring job method");
        }
    }

    public static class MyUnsupportedService {

        @Recurring(id = "my-recurring-job", cron = "0 0/15 * * *")
        public void myRecurringMethod(String parameter) {
            System.out.print("My unsupported recurring job method");
        }
    }
}