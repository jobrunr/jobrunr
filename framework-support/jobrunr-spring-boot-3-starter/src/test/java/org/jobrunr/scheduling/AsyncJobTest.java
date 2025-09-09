package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {JobRunrAutoConfiguration.class, AsyncJobTest.AsyncJobTestContextConfiguration.class})
public class AsyncJobTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncJobTest.class);

    @Autowired
    AsyncJobTestService asyncJobTestService;

    @Autowired
    AsyncJobTestServiceWithNestedJobService asyncJobTestServiceWithNestedJobService;

    @Autowired
    InMemoryStorageProvider storageProvider;

    @MockitoSpyBean
    JobScheduler jobScheduler;

    @BeforeEach
    public void clearInMemoryStorage() {
        storageProvider.clear();
        clearInvocations(jobScheduler);
    }

    @Test
    public void testAsyncJob() {
        asyncJobTestService.testMethodAsAsyncJob();
        await().atMost(30, TimeUnit.SECONDS).until(() -> storageProvider.countJobs(StateName.SUCCEEDED) == 1);
    }

    @Test
    public void testNestedAsyncJob() {
        asyncJobTestServiceWithNestedJobService.testMethodThatCreatesOtherJobsAsAsyncJob();
        await().atMost(30, TimeUnit.SECONDS).until(() -> storageProvider.countJobs(StateName.SUCCEEDED) == 2);
    }

    @Test
    public void testAsyncJobThatCallsAnotherAsyncMethodFromSameObject() {
        asyncJobTestService.testMethodThatCallsAnotherAsyncJobMethodFromSameObject();
        await().atMost(30, TimeUnit.SECONDS).until(() -> storageProvider.countJobs(StateName.SUCCEEDED) == 1);
        verify(jobScheduler).enqueue(eq(null), Mockito.any(JobDetails.class));
    }


    @AsyncJob
    public static class AsyncJobTestService {

        @Job(name = "my async spring job")
        public void testMethodAsAsyncJob() {
            LOGGER.info("Running AsyncJobService.testMethodAsAsyncJob in a job");
        }

        @Job(name = "my async spring job that calls another async method from same object")
        public void testMethodThatCallsAnotherAsyncJobMethodFromSameObject() {
            LOGGER.info("Running AsyncJobTestServiceWithNestedJobService.testMethodThatCallsAnotherAsyncJobMethodFromSameObject in a job. It will not create another job.");
            this.testMethodAsAsyncJob();
        }
    }

    @AsyncJob
    public static class AsyncJobTestServiceWithNestedJobService {

        private final AsyncJobTestService asyncJobTestService;

        public AsyncJobTestServiceWithNestedJobService(AsyncJobTestService asyncJobTestService) {
            this.asyncJobTestService = asyncJobTestService;
        }

        @Job(name = "my async spring job with nested jobs")
        public void testMethodThatCreatesOtherJobsAsAsyncJob() {
            LOGGER.info("Running AsyncJobTestServiceWithNestedJobService.testMethodThatCreatesOtherJobsAsAsyncJob in a job. It will create another job.");
            asyncJobTestService.testMethodAsAsyncJob();
        }
    }

    static class AsyncJobTestContextConfiguration {

        @Bean
        public InMemoryStorageProvider storageProvider() {
            InMemoryStorageProvider inMemoryStorageProvider = new InMemoryStorageProvider();
            inMemoryStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
            return inMemoryStorageProvider;
        }

        @Bean
        public AsyncJobTestService asyncJobService() {
            return new AsyncJobTestService();
        }

        @Bean
        public AsyncJobTestServiceWithNestedJobService asyncJobServiceWithNestedJobService(AsyncJobTestService asyncJobTestService) {
            return new AsyncJobTestServiceWithNestedJobService(asyncJobTestService);
        }

    }
}
