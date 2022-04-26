package org.jobrunr.jobs.mappers;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.*;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.runner.RunnerJobContext;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.mapper.JobParameterJsonMapperException;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.*;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobParameterThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
abstract class JobMapperTest {

    @Mock
    private BackgroundJobServer backgroundJobServer;

    private TestService testService;

    private JobMapper jobMapper;

    @BeforeEach
    void setUp() {
        jobMapper = new JobMapper(getJsonMapper());
        testService = new TestService();

        lenient().when(backgroundJobServer.getId()).thenReturn(UUID.randomUUID());
    }

    protected abstract JsonMapper getJsonMapper();

    @Test
    void testSerializeAndDeserializeJobWithVersion() {
        Job job = anEnqueuedJob()
                .withVersion(2)
                .build();

        String jobAsString = jobMapper.serializeJob(job);
        final Job actualJob = jobMapper.deserializeJob(jobAsString);

        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeProcessingJobWithLogs() {
        Job job = anEnqueuedJob().withState(new ProcessingState(UUID.randomUUID())).build();
        final RunnerJobContext jobContext = new RunnerJobContext(job);
        jobContext.logger().info("test 1");
        jobContext.logger().warn("test 2");
        jobContext.progressBar(10).setValue(4);

        String jobAsString = jobMapper.serializeJob(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/jobs/mappers/job-in-progress-with-logs-and-progressbar.json"));
        final Job actualJob = jobMapper.deserializeJob(jobAsString);

        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeJobWithPath() {
        Job job = anEnqueuedJob().withJobDetails(() -> testService.doWorkWithPath(Paths.get("/tmp", "jobrunr", "log.txt"))).build();

        String jobAsString = jobMapper.serializeJob(job);

        assertThatCode(() -> jobMapper.deserializeJob(jobAsString)).doesNotThrowAnyException();
    }

    @Test
    void serializeAndDeserializeRecurringJob() {
        RecurringJob recurringJob = aDefaultRecurringJob().build();

        String jobAsString = jobMapper.serializeRecurringJob(recurringJob);
        final RecurringJob actualRecurringJob = jobMapper.deserializeRecurringJob(jobAsString);

        assertThat(actualRecurringJob).isEqualTo(recurringJob);
    }

    @Test
    void canSerializeAndDeserializeWithJobContext() {
        Job job = anEnqueuedJob()
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWork")
                        .withJobParameter(5)
                        .withJobParameter(JobParameter.JobContext)
                )
                .build();

        String jobAsJson = jobMapper.serializeJob(job);
        Job actualJob = jobMapper.deserializeJob(jobAsJson);

        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void canSerializeAndDeserializeJobWithAllStatesAndMetadata() {
        Job job = anEnqueuedJob()
                .withMetadata("metadata1", new TestMetadata("input"))
                .withMetadata("metadata2", "a string")
                .withMetadata("metadata3", 15.1)
                .withMetadata("metadata6", 16.0)
                .withMetadata("metadata7", true)
                .build();
        job.startProcessingOn(backgroundJobServer);
        job.failed("exception", new Exception("Test"));
        job.enqueue();
        job.succeeded();

        String jobAsJson = jobMapper.serializeJob(job);
        Job actualJob = jobMapper.deserializeJob(jobAsJson);

        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void onIllegalJobParameterCorrectExceptionIsThrown() {
        TestService.IllegalWork illegalWork = new TestService.IllegalWork(5);
        Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doIllegalWork(illegalWork))
                .build();

        assertThatCode(() -> jobMapper.serializeJob(job))
                .isInstanceOf(JobParameterJsonMapperException.class)
                .isNotExactlyInstanceOf(JobRunrException.class);
    }

    @Test
    void onJobWithParameterThatCannotBeDeserializedAnymoreNoExceptionIsThrown() {
        Job job = anEnqueuedJob()
                .withJobDetails(jobParameterThatDoesNotExistJobDetails())
                .build();

        String jobAsJson = jobMapper.serializeJob(job);
        Job actualJob = jobMapper.deserializeJob(jobAsJson);

        assertThat(actualJob).isNotNull();

        assertThat(actualJob.getJobDetails())
                .hasClassName(TestService.class.getName())
                .hasMethodName("doWork")
                .hasArg(arg -> arg instanceof JobParameterNotDeserializableException
                                && ((JobParameterNotDeserializableException) arg).getClassName().equals("i.dont.exist.Class"));

        String jobAsJsonAfterDeserialization = jobMapper.serializeJob(actualJob);
        Job actualJobAfterDeserialization = jobMapper.deserializeJob(jobAsJsonAfterDeserialization);
        assertThat(actualJobAfterDeserialization.getJobDetails())
                .hasClassName(TestService.class.getName())
                .hasMethodName("doWork")
                .hasArg(arg -> arg instanceof JobParameterNotDeserializableException
                        && ((JobParameterNotDeserializableException) arg).getClassName().equals("i.dont.exist.Class"));
    }

    public static class TestMetadata implements JobContext.Metadata {
        private String input;
        private Instant instant;
        private Path path;
        private File file;

        protected TestMetadata() {
        }

        public TestMetadata(String input) {
            this.input = input;
            this.instant = Instant.now();
            this.path = Paths.get("/tmp");
            this.file = new File("/tmp");
        }

        public String getInput() {
            return input;
        }

        public Instant getInstant() {
            return instant;
        }

        public Path getPath() {
            return path;
        }

        public File getFile() {
            return file;
        }
    }
}