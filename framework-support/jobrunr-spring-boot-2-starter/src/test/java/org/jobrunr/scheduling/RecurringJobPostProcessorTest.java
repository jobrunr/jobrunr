package org.jobrunr.scheduling;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.RecurringJob.CreatedBy.ANNOTATION;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RecurringJobPostProcessorTest {

    @Mock
    private BeanFactory beanFactory;
    @Mock
    private JobScheduler jobScheduler;

    @Captor
    private ArgumentCaptor<RecurringJob> recurringJobArgumentCaptor;

    @BeforeEach
    void setUpJobSchedulerObjectFactory() {
        lenient().when(beanFactory.getBean(JobScheduler.class)).thenReturn(jobScheduler);
    }

    @Test
    void beansWithoutMethodsAnnotatedWithRecurringAnnotationWillNotBeHandled() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = getRecurringJobPostProcessor();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithoutRecurringAnnotation(), "not important");

        // THEN
        verifyNoInteractions(jobScheduler);
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationWillAutomaticallyBeRegistered() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = getRecurringJobPostProcessor();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringJob(), "not important");

        // THEN
        verify(jobScheduler).scheduleRecurrently(recurringJobArgumentCaptor.capture());

        final RecurringJob actualRecurringJob = recurringJobArgumentCaptor.getValue();
        assertThat(actualRecurringJob)
                .hasId("my-recurring-job")
                .hasScheduleExpression("0 0/15 * * *")
                .hasCreatedBy(ANNOTATION);
        assertThat(actualRecurringJob.getJobDetails())
                .isCacheable()
                .hasClassName(MyServiceWithRecurringJob.class.getName())
                .hasMethodName("myRecurringMethod")
                .hasNoArgs();
    }

    @Test
    void beansWithMethodsUsingJobContextAnnotatedWithRecurringCronAnnotationWillAutomaticallyBeRegistered() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = getRecurringJobPostProcessor();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringCronJobUsingJobContext(), "not important");

        // THEN
        verify(jobScheduler).scheduleRecurrently(recurringJobArgumentCaptor.capture());

        final RecurringJob actualRecurringJob = recurringJobArgumentCaptor.getValue();
        assertThat(actualRecurringJob)
                .hasId("my-recurring-job")
                .hasScheduleExpression("0 0/15 * * *")
                .hasCreatedBy(ANNOTATION);
        assertThat(actualRecurringJob.getJobDetails())
                .isCacheable()
                .hasClassName(MyServiceWithRecurringCronJobUsingJobContext.class.getName())
                .hasMethodName("myRecurringMethod")
                .hasJobContextArg();
    }

    @Test
    void beansWithMethodsUsingJobContextAnnotatedWithRecurringIntervalAnnotationWillAutomaticallyBeRegistered() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = getRecurringJobPostProcessor();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringIntervalJobUsingJobContext(), "not important");

        // THEN
        verify(jobScheduler).scheduleRecurrently(recurringJobArgumentCaptor.capture());

        final RecurringJob actualRecurringJob = recurringJobArgumentCaptor.getValue();
        assertThat(actualRecurringJob)
                .hasId("my-recurring-job")
                .hasScheduleExpression("PT10M");
        assertThat(actualRecurringJob.getJobDetails())
                .isCacheable()
                .hasClassName(MyServiceWithRecurringIntervalJobUsingJobContext.class.getName())
                .hasMethodName("myRecurringMethod")
                .hasJobContextArg();
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationCronDisabled() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = getRecurringJobPostProcessor();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringCronJobDisabled(), "not important");

        // THEN
        verify(jobScheduler).deleteRecurringJob("my-recurring-job");
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationIntervalDisabled() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = getRecurringJobPostProcessor();

        // WHEN
        recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringIntervalDisabled(), "not important");

        // THEN
        verify(jobScheduler).deleteRecurringJob("my-recurring-job");
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationCronAndIntervalWillThrowException() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = getRecurringJobPostProcessor();

        // WHEN & THEN
        assertThatThrownBy(() -> recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringJobWithCronAndInterval(), "not important"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationNoCronOrIntervalWillThrowException() {
        // GIVEN
        final RecurringJobPostProcessor recurringJobPostProcessor = getRecurringJobPostProcessor();

        // WHEN & THEN
        assertThatThrownBy(() -> recurringJobPostProcessor.postProcessAfterInitialization(new MyServiceWithRecurringJobWithoutCronAndInterval(), "not important"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationContainingPropertyPlaceholdersWillBeResolved() {
        new ApplicationContextRunner()
                .withBean(RecurringJobPostProcessor.class)
                .withBean(JobScheduler.class, () -> jobScheduler)
                .withPropertyValues("my-job.id=my-recurring-job")
                .withPropertyValues("my-job.cron=0 0/15 * * *")
                .withPropertyValues("my-job.zone-id=Asia/Taipei")
                .run(context -> {
                    context.getBean(RecurringJobPostProcessor.class)
                            .postProcessAfterInitialization(new MyServiceWithRecurringAnnotationContainingPropertyPlaceholder(), "not important");

                    verify(jobScheduler).scheduleRecurrently(recurringJobArgumentCaptor.capture());
                    assertThat(recurringJobArgumentCaptor.getValue())
                            .hasId("my-recurring-job")
                            .hasScheduleExpression("0 0/15 * * *")
                            .hasZoneId("Asia/Taipei");
                });
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationHasDisabledCronExpressionValueShouldBeDeleted() {
        new ApplicationContextRunner()
                .withBean(RecurringJobPostProcessor.class)
                .withBean(JobScheduler.class, () -> jobScheduler)
                .withPropertyValues("my-job.id=my-recurring-job-to-be-deleted")
                .withPropertyValues("my-job.cron=-")
                .withPropertyValues("my-job.zone-id=Asia/Taipei")
                .run(context -> {
                    context.getBean(RecurringJobPostProcessor.class)
                            .postProcessAfterInitialization(new MyServiceWithRecurringAnnotationContainingPropertyPlaceholder(), "not important");

                    verify(jobScheduler).deleteRecurringJob("my-recurring-job-to-be-deleted");
                });
    }

    @Test
    void beansWithMethodsAnnotatedWithRecurringAnnotationHasDisabledCronExpressionButNotSpecifiedIdShouldBeOmitted() {
        new ApplicationContextRunner()
                .withBean(RecurringJobPostProcessor.class)
                .withBean(JobScheduler.class, () -> jobScheduler)
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
        final RecurringJobPostProcessor recurringJobPostProcessor = getRecurringJobPostProcessor();

        assertThatThrownBy(() -> recurringJobPostProcessor.postProcessAfterInitialization(new MyUnsupportedService(), "not important"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Methods annotated with " + Recurring.class.getName() + " can only have zero parameters or a single parameter of type JobContext.");
    }


    private RecurringJobPostProcessor getRecurringJobPostProcessor() {
        final RecurringJobPostProcessor recurringJobPostProcessor = new RecurringJobPostProcessor();
        recurringJobPostProcessor.setBeanFactory(beanFactory);
        recurringJobPostProcessor.afterPropertiesSet();
        return recurringJobPostProcessor;
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
