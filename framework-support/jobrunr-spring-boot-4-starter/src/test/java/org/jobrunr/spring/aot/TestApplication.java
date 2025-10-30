package org.jobrunr.spring.aot;

import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;

@SpringBootTest(classes = {JobRunrAutoConfiguration.class, TestApplication.JobRunrStorageProviderTestContextConfiguration.class})
public class TestApplication {

    @Autowired
    JobRequestScheduler jobRequestScheduler;

    @Autowired
    StorageProvider storageProvider;

    @TestConfiguration
    @ComponentScan("org.jobrunr.spring.aot")
    static class JobRunrStorageProviderTestContextConfiguration {

        @Bean
        @Primary
        public JsonMapper jsonMapper() {
            return new JacksonJsonMapper();
        }

        @Bean
        public StorageProvider storageProvider(JsonMapper jsonMapper) {
            InMemoryStorageProvider inMemoryStorageProvider = new InMemoryStorageProvider();
            inMemoryStorageProvider.setJobMapper(new JobMapper(jsonMapper));
            return inMemoryStorageProvider;
        }
    }

    @Test
    void testEnqueue() {
        JobId jobId = jobRequestScheduler.enqueue(new SysoutJobRequest("Hello from SysoutJobRequest"));

        await().atMost(30, TimeUnit.SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(SUCCEEDED));
    }
}
