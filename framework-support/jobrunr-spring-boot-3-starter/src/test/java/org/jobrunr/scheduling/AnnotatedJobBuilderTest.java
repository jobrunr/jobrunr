package org.jobrunr.scheduling;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.JobGateway;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnnotatedJobBuilderTest {

    private static final Logger log = LoggerFactory.getLogger(AnnotatedJobBuilderTest.class);
    @Mock
    private JobScheduler jobScheduler;

    @Mock
    private BeanFactory beanFactory;

    @Captor
    private ArgumentCaptor<JobLambda> jobLambdaArgumentCaptor;

    @BeforeEach
    void setUpJobSchedulerObjectFactory() {
        lenient().when(beanFactory.getBean(JobScheduler.class)).thenReturn(jobScheduler);
    }

    @Test
    void classAnnotatedWithJobGatewayAnnotationWillAutomaticallyBeRegistered() throws Exception {
        MyServiceWithJobGatewayAnnotation myService = new MyServiceWithJobGatewayAnnotation();
        final JobGatewayPostProcessor jobGatewayPostProcessor = getJobGatewayPostProcessor();
        jobGatewayPostProcessor.postProcessAfterInitialization(myService, "myService");
        verify(jobScheduler, times(2)).enqueue(jobLambdaArgumentCaptor.capture());

        jobLambdaArgumentCaptor
                .getAllValues()
                .forEach(jobLambda -> {
                    log.info("JobLambda: {}", jobLambda);
                    try {
                        jobLambda.run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private JobGatewayPostProcessor getJobGatewayPostProcessor() {
        final JobGatewayPostProcessor jobGatewayPostProcessor = new JobGatewayPostProcessor();
        jobGatewayPostProcessor.setBeanFactory(beanFactory);
        jobGatewayPostProcessor.afterPropertiesSet();
        return jobGatewayPostProcessor;
    }

    @JobGateway
    public static class MyServiceWithJobGatewayAnnotation {
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
