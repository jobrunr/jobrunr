package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.jobrunr.spring.annotations.Recurring;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RecurringJobPostProcessorTest {

    @Mock
    private JobScheduler jobScheduler;

    @Captor
    private ArgumentCaptor<JobDetails> jobDetailsArgumentCaptor;

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
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);
        recurringJobPostProcessor.afterPropertiesSet();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringJob(), "not important");

        // THEN
        verify(jobScheduler).scheduleRecurrently(eq("my-recurring-job"), jobDetailsArgumentCaptor.capture(), eq(CronExpression.create("0 0/15 * * *")), any(ZoneId.class));

        final JobDetails actualJobDetails = jobDetailsArgumentCaptor.getValue();
        assertThat(actualJobDetails.getClassName()).isEqualTo(MyServiceWithRecurringJob.class.getName());
        assertThat(actualJobDetails.getMethodName()).isEqualTo("myRecurringMethod");
        assertThat(actualJobDetails.getCacheable()).isTrue();
    }

    @Test
    void beansWithMethodsUsingJobContextAnnotatedWithRecurringCronAnnotationWillAutomaticallyBeRegistered() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);
        recurringJobPostProcessor.afterPropertiesSet();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringCronJobUsingJobContext(), "not important");

        // THEN
        verify(jobScheduler).scheduleRecurrently(eq("my-recurring-job"), jobDetailsArgumentCaptor.capture(), eq(CronExpression.create("0 0/15 * * *")), any(ZoneId.class));

        final JobDetails actualJobDetails = jobDetailsArgumentCaptor.getValue();
        assertThat(actualJobDetails.getClassName()).isEqualTo(MyServiceWithRecurringCronJobUsingJobContext.class.getName());
        assertThat(actualJobDetails.getMethodName()).isEqualTo("myRecurringMethod");
        assertThat(actualJobDetails.getCacheable()).isTrue();
    }

    @Test
    void beansWithMethodsUsingJobContextAnnotatedWithRecurringIntervalAnnotationWillAutomaticallyBeRegistered() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);
        recurringJobPostProcessor.afterPropertiesSet();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringIntervalJobUsingJobContext(), "not important");

        // THEN
        verify(jobScheduler).scheduleRecurrently(eq("my-recurring-job"), jobDetailsArgumentCaptor.capture(), eq(new Interval("PT10M")), any(ZoneId.class));

        final JobDetails actualJobDetails = jobDetailsArgumentCaptor.getValue();
        assertThat(actualJobDetails.getClassName()).isEqualTo(MyServiceWithRecurringIntervalJobUsingJobContext.class.getName());
        assertThat(actualJobDetails.getMethodName()).isEqualTo("myRecurringMethod");
        assertThat(actualJobDetails.getCacheable()).isTrue();
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationCronDisabled() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);
        recurringJobPostProcessor.afterPropertiesSet();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringCronJobDisabled(), "not important");

        // THEN
        verify(jobScheduler).delete("my-recurring-job");
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationIntervalDisabled() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);
        recurringJobPostProcessor.afterPropertiesSet();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringIntervalDisabled(), "not important");

        // THEN
        verify(jobScheduler).delete("my-recurring-job");
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationCronAndIntervalWillThrowException() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);
        recurringJobPostProcessor.afterPropertiesSet();

        // WHEN & THEN
        assertThatThrownBy(() -> recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringJobWithCronAndInterval(), "not important"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationNoCronOrIntervalWillThrowException() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);
        recurringJobPostProcessor.afterPropertiesSet();

        // WHEN & THEN
        assertThatThrownBy(() -> recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringJobWithoutCronAndInterval(), "not important"))
                .isInstanceOf(IllegalArgumentException.class);
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
    void beansWithMethodsAnnotatedWithRecurringAnnotationHasDisabledCronExpressionValueShouldBeDeleted() {
        new ApplicationContextRunner()
                .withBean(RecurringJobPostProcessor.class, jobScheduler)
                .withPropertyValues("my-job.id=my-recurring-job-to-be-deleted")
                .withPropertyValues("my-job.cron=-")
                .withPropertyValues("my-job.zone-id=Asia/Taipei")
                .run(context -> {
                    context.getBean(RecurringJobPostProcessor.class)
                            .postProcessAfterInitialization(new MyServiceWithRecurringAnnotationContainingPropertyPlaceholder(), "not important");

                    verify(jobScheduler).delete("my-recurring-job-to-be-deleted");
                });
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationHasDisabledCronExpressionButNotSpecifiedIdShouldBeOmitted() {
        new ApplicationContextRunner()
                .withBean(RecurringJobPostProcessor.class, jobScheduler)
                .withPropertyValues("my-job.id=")
                .withPropertyValues("my-job.cron=-")
                .withPropertyValues("my-job.zone-id=Asia/Taipei")
                .run(context -> {
                    context.getBean(RecurringJobPostProcessor.class)
                            .postProcessAfterInitialization(new MyServiceWithRecurringAnnotationContainingPropertyPlaceholder(), "not important");

                    verifyNoInteractions(jobScheduler);
                });
    }

    @Test
    void beansWithUnsupportedMethodsAnnotatedWithRecurringAnnotationWillThrowException() {
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor(jobScheduler);
        recurringJobPostProcessor.afterPropertiesSet();

        assertThatThrownBy(() -> recurringJobPostProcessor.postProcessAfterInitialization(new MyUnsupportedService(), "not important"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Methods annotated with " + Recurring.class.getName() + " can only have zero parameters or a single parameter of type JobContext.");
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

    public static class MyServiceWithRecurringCronJobUsingJobContext {

        @Recurring(id = "my-recurring-job", cron = "0 0/15 * * *")
        public void myRecurringMethod(JobContext jobContext) {
            System.out.print("My recurring job method");
        }
    }

    public static class MyServiceWithRecurringIntervalJobUsingJobContext {

        @Recurring(id = "my-recurring-job", interval = "PT10M")
        public void myRecurringMethod(JobContext jobContext) {
            System.out.print("My recurring job method");
        }
    }

    public static class MyServiceWithRecurringJobWithoutCronAndInterval {

        @Recurring(id = "my-recurring-job")
        public void myRecurringMethod(JobContext jobContext) {
            System.out.print("My recurring job method");
        }
    }

    public static class MyServiceWithRecurringJobWithCronAndInterval {

        @Recurring(id = "my-recurring-job", cron = "0 0/15 * * *", interval = "PT10M")
        public void myRecurringMethod(JobContext jobContext) {
            System.out.print("My recurring job method");
        }
    }

    public static class MyServiceWithRecurringCronJobDisabled {

        @Recurring(id = "my-recurring-job", cron = "-")
        public void myRecurringMethod() {
            System.out.print("My recurring job method");
        }
    }

    public static class MyServiceWithRecurringIntervalDisabled {

        @Recurring(id = "my-recurring-job", interval = "-")
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
