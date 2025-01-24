package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
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

import static org.junit.jupiter.api.Assertions.*;
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

        List<String> enqueuedMethods = jobDetailsArgumentCaptor
                .getAllValues()
                .stream()
                .map(JobDetails::getMethodName)
                .sorted()
                .toList();

        List<List<JobParameter>> enqueuedJobParams = jobDetailsArgumentCaptor
                .getAllValues()
                .stream()
                .map(JobDetails::getJobParameters)
                .toList();

        assertEquals(2, enqueuedMethods.size());
        assertTrue(enqueuedMethods.contains("myMethodWithJobAnnotation1"));
        assertTrue(enqueuedMethods.contains("myMethodWithJobAnnotation2"));
        assertFalse(enqueuedMethods.contains("myMethodWithoutJobAnnotation"));
        assertEquals(0, enqueuedJobParams.get(0).size());
        assertEquals(1, enqueuedJobParams.get(1).size());
        assertEquals("test-input", enqueuedJobParams.get(1).get(0).getObject());
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