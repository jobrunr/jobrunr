package org.jobrunr.spring.aot;

import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {JobRunrAutoConfiguration.class, AsyncJobTest.AsyncJobTestContextConfiguration.class})
public class AsyncJobTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncJobTest.class);

    @Autowired
    AsyncJobTestService asyncJobTestService;

    @Autowired
    StorageProvider storageProvider;

    @AsyncJob
    public static class AsyncJobTestService {

        @Job(name = "my async spring job")
        public void testMethodAsAsyncJob() {
            LOGGER.info("Running AsyncJobService.testMethodAsAsyncJob in a job");
        }

    }

    @TestConfiguration
    static class AsyncJobTestContextConfiguration {
        @Bean
        public StorageProvider storageProvider() {
            InMemoryStorageProvider inMemoryStorageProvider = new InMemoryStorageProvider();
            inMemoryStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
            return inMemoryStorageProvider;
        }

        @Bean
        public AsyncJobTestService asyncJobService() {
            return new AsyncJobTestService();
        }
    }

    @Test
    public void testAsyncJob() {
        asyncJobTestService.testMethodAsAsyncJob();
        await().until(() -> storageProvider.countJobs(StateName.SUCCEEDED) == 1);
    }
}
