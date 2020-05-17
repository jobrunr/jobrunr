package org.jobrunr.jobs.mappers;

import org.jobrunr.jobs.JobContext;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class JobMapperUsingJacksonMapperTest {

    private JobMapper jobMapper;

    @Mock
    private BackgroundJobServer backgroundJobServer;

    @BeforeEach
    public void setUp() {
        jobMapper = new JobMapper(new JacksonJsonMapper());

        lenient().when(backgroundJobServer.getId()).thenReturn(UUID.randomUUID());
    }

    @Test
    public void canSerializeAndDeserialize() {
        org.jobrunr.jobs.Job job = anEnqueuedJob().build();

        String jobAsJson = jobMapper.serializeJob(job);
        org.jobrunr.jobs.Job actualJob = jobMapper.deserializeJob(jobAsJson);

        assertThat(actualJob).usingRecursiveComparison().isEqualTo(job);
    }

    @Test
    public void canSerializeAndDeserializeWithJobContext() {
        org.jobrunr.jobs.Job job = anEnqueuedJob()
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWork")
                        .withJobParameter(5)
                        .withJobParameter(JobParameter.JobContext)
                )
                .build();

        String jobAsJson = jobMapper.serializeJob(job);
        org.jobrunr.jobs.Job actualJob = jobMapper.deserializeJob(jobAsJson);

        assertThat(actualJob).usingRecursiveComparison().isEqualTo(job);
    }

    @Test
    public void canSerializeAndDeserializeJobWithAllStatesAndMetadata() {
        org.jobrunr.jobs.Job job = anEnqueuedJob()
                .withMetadata("metadata1", new TestMetadata("input"))
                .withMetadata("metadata2", "a string")
                .withMetadata("metadata3", 15)
                .build();
        job.startProcessingOn(backgroundJobServer);
        job.failed("exception", new Exception("Test"));
        job.succeeded();

        String jobAsJson = jobMapper.serializeJob(job);
        org.jobrunr.jobs.Job actualJob = jobMapper.deserializeJob(jobAsJson);

        assertThat(actualJob).usingRecursiveComparison().isEqualTo(job);
    }

    public static class TestMetadata implements JobContext.Metadata {
        private String input;
        private Instant instant;

        private TestMetadata() {
        }

        public TestMetadata(String input) {
            this.input = input;
            this.instant = Instant.now();
        }

        public String getInput() {
            return input;
        }

        public Instant getInstant() {
            return instant;
        }
    }

}