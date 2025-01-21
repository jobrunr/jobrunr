package org.jobrunr.scheduling;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AsyncJobPostPostProcessorTest.class);

    @Test
    void classAnnotatedWithAsyncJobAnnotationWillAutomaticallyBeRegistered() throws Exception {
        MyServiceWithAsyncJobAnnotation myService = new MyServiceWithAsyncJobAnnotation();
        final AsyncJobPostProcessor asyncJobPostProcessor = getAsyncJobPostProcessor();
        MyServiceWithAsyncJobAnnotation myServicePostInitialisation = (MyServiceWithAsyncJobAnnotation) asyncJobPostProcessor.postProcessBeforeInitialization(myService, "myService");
        myServicePostInitialisation.myMethodWithJobAnnotation1();
        myServicePostInitialisation.myMethodWithJobAnnotation2();
        myServicePostInitialisation.myMethodWithoutJobAnnotation();
        verify(jobScheduler, times(2)).enqueue(ArgumentMatchers.isNull(), jobDetailsArgumentCaptor.capture());

        List<String> enqueuedMethods = jobDetailsArgumentCaptor
                .getAllValues()
                .stream()
                .map(JobDetails::getMethodName)
                .sorted()
                .toList();

        assertEquals(2, enqueuedMethods.size());
        assertTrue(enqueuedMethods.contains("myMethodWithJobAnnotation1"));
        assertTrue(enqueuedMethods.contains("myMethodWithJobAnnotation2"));
        assertFalse(enqueuedMethods.contains("myMethodWithoutJobAnnotation"));

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
        public void myMethodWithJobAnnotation2() {
            System.out.print("My method with Job annotation 2");
        }

        public void myMethodWithoutJobAnnotation() {
            System.out.print("My method without Job annotation");
        }

    }

}
