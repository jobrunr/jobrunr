package org.jobrunr.scheduling;

import org.jobrunr.annotations.Recurring;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.scheduling.cron.CronExpression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
        recurringJobPostProcessor.afterPropertiesSet();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithoutRecurringAnnotation(), "not important");

        // THEN
        verifyNoInteractions(jobScheduler);
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationWillAutomaticallyBeRegistered() {
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);
        recurringJobPostProcessor.afterPropertiesSet();

        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringJob(), "not important");

        verify(jobScheduler).scheduleRecurrently(eq("my-recurring-job"), any(JobDetails.class), eq(CronExpression.create("0 0/15 * * *")), any(ZoneId.class));
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationContainingPropertyPlaceholdersWillBeResolved() {
        new ApplicationContextRunner()
                .withBean(RecurringJobPostProcessor.class, jobScheduler)
                .withPropertyValues("my-job.id=my-recurring-job")
                .withPropertyValues("my-job.cron=0 0/15 * * *")
                .withPropertyValues("my-job.zone-id=Asia/Taipei")
                .run(context -> {
                    context.getBean(RecurringJobPostProcessor.class)
                    .postProcessAfterInitialization(new MyServiceWithRecurringAnnotationContainingPropertyPlaceholder(), "not important");

                    verify(jobScheduler).scheduleRecurrently(eq("my-recurring-job"), any(JobDetails.class), eq(CronExpression.create("0 0/15 * * *")), eq(ZoneId.of("Asia/Taipei")));
                });
    }

    @Test
    void beansWithUnsupportedMethodsAnnotatedWithRecurringAnnotationWillThrowException() {
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);
        recurringJobPostProcessor.afterPropertiesSet();

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

    public static class MyServiceWithRecurringAnnotationContainingPropertyPlaceholder {

        @Recurring(id = "${my-job.id}", cron = "${my-job.cron}", zoneId = "${my-job.zone-id}")
        public void myRecurringMethod() {
            System.out.print("My recurring job method");
        }
    }
}
