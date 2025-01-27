package org.jobrunr.scheduling;

import org.jobrunr.JobRunrAssertions;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AsyncJobPostPostProcessorTest {
    private JobScheduler jobScheduler;

    private BeanFactory beanFactory;

    @Captor
    private ArgumentCaptor<JobDetails> jobDetailsArgumentCaptor;

    @BeforeEach
    void setUpJobSchedulerObjectFactory() {
        this.jobScheduler = mock(JobScheduler.class);
        this.beanFactory = mock(BeanFactory.class);
        when(beanFactory.getBean(JobScheduler.class)).thenReturn(jobScheduler);
    }

    @Test
    void classAnnotatedWithAsyncJobAnnotationWillAutomaticallyBeRegistered() {
        MyServiceWithAsyncJobAnnotation myService = new MyServiceWithAsyncJobAnnotation();
        final AsyncJobPostProcessor asyncJobPostProcessor = getAsyncJobPostProcessor();
        MyServiceWithAsyncJobAnnotation myServicePostInitialisation = (MyServiceWithAsyncJobAnnotation) asyncJobPostProcessor.postProcessBeforeInitialization(myService, "myService");
        myServicePostInitialisation.myMethodWithJobAnnotation1();
        myServicePostInitialisation.myMethodWithJobAnnotation2("test-input");
        myServicePostInitialisation.myMethodWithoutJobAnnotation();
        verify(jobScheduler, times(2)).enqueue(ArgumentMatchers.isNull(), jobDetailsArgumentCaptor.capture());
        List<JobDetails> jobDetails = jobDetailsArgumentCaptor.getAllValues();
        JobRunrAssertions.assertThat(jobDetails.get(0))
                .hasClass(MyServiceWithAsyncJobAnnotation.class)
                .hasMethodName("myMethodWithJobAnnotation1")
                .hasNoArgs();
        JobRunrAssertions.assertThat(jobDetails.get(1))
                .hasClass(MyServiceWithAsyncJobAnnotation.class)
                .hasMethodName("myMethodWithJobAnnotation2")
                .hasArgs("test-input");
    }

    private AsyncJobPostProcessor getAsyncJobPostProcessor() {
        final AsyncJobPostProcessor asyncJobPostProcessor = new AsyncJobPostProcessor();
        asyncJobPostProcessor.setBeanFactory(beanFactory);
        return asyncJobPostProcessor;
    }

    @AsyncJob
    public static class MyServiceWithAsyncJobAnnotation {
        @Job(name = "my-annotated-job-1")
        public void myMethodWithJobAnnotation1() {
            System.out.print("My method with Job annotation 1");
        }

        @Job(name = "my-annotated-job-2")
        public void myMethodWithJobAnnotation2(String input) {
            System.out.print("My method with Job annotation 2 -" + input);
        }

        public void myMethodWithoutJobAnnotation() {
            System.out.print("My method without Job annotation");
        }

    }

}