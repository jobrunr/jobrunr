package org.jobrunr.jobs.mappers;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.exceptions.JobParameterNotDeserializableException;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.runner.RunnerJobContext;
import org.jobrunr.stubs.Mocks;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.mapper.JobParameterJsonMapperException;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.JsonMapperValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThatJson;
import static org.jobrunr.JobRunrAssertions.contentOfResource;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobParameterThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;

@ExtendWith(MockitoExtension.class)
public abstract class JobMapperTest {

    private BackgroundJobServer backgroundJobServer = Mocks.ofBackgroundJobServer();

    private TestService testService;

    protected JobMapper jobMapper;

    @BeforeEach
    void setUp() {
        jobMapper = new JobMapper(getJsonMapper());
        testService = new TestService();
    }

    protected abstract JsonMapper getJsonMapper();

    @Test
    void testValidateJsonMapper() {
        assertThatCode(() -> JsonMapperValidator.validateJsonMapper(getJsonMapper()))
                .doesNotThrowAnyException();
    }

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
        Job job = anEnqueuedJob().withState(new ProcessingState(UUID.randomUUID(), "not important")).build();
        final RunnerJobContext jobContext = new RunnerJobContext(job);
        jobContext.logger().info("test 1");
        jobContext.logger().warn("test 2");
        jobContext.progressBar(10).setProgress(4);

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
    void testSerializeAndDeserializeJobWithLabel() {
        Job job = anEnqueuedJob().withLabels("first label", "second label").build();

        String jobAsString = jobMapper.serializeJob(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/jobs/mappers/enqueued-job-with-labels.json"));

        final Job actualJob = jobMapper.deserializeJob(jobAsString);
        assertThat(actualJob).isEqualTo(job);
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
                        .withJobParameter(JobContext.NULL)
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
        job.scheduleAt(Instant.now(), "Retry 1");
        job.enqueue();
        job.startProcessingOn(backgroundJobServer);

        String jobAsJson = jobMapper.serializeJob(job);
        Job actualJob = jobMapper.deserializeJob(jobAsJson);

        assertThat(actualJob)
                .isEqualTo(job)
                .hasMetadata("metadata1")
                .hasMetadata("metadata2", "a string")
                .hasMetadata("metadata3", 15.1)
                .hasMetadata("metadata6", 16.0)
                .hasMetadata("metadata7", true);
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
                .hasNotDeserializableExceptionEqualTo(new JobParameterNotDeserializableException("i.dont.exist.Class", "java.lang.IllegalArgumentException", "Class not found: i.dont.exist.Class"));

        String jobAsJsonAfterDeserialization = jobMapper.serializeJob(actualJob);
        Job actualJobAfterDeserialization = jobMapper.deserializeJob(jobAsJsonAfterDeserialization);
        assertThat(actualJobAfterDeserialization.getJobDetails())
                .hasClassName(TestService.class.getName())
                .hasMethodName("doWork")
                .hasNotDeserializableExceptionEqualTo(new JobParameterNotDeserializableException("i.dont.exist.Class", "java.lang.IllegalArgumentException", "Class not found: i.dont.exist.Class"));
    }

    @Test
    void canSerializeAndDeserializeWithStepResult() {
        // GIVEN
        Job job = anEnqueuedJob().build();
        job.startProcessingOn(backgroundJobServer);
        RunnerJobContext jobContext = new RunnerJobContext(job);
        String inputString = jobContext.runStepOnce("step-1", () -> "result-1");
        UUID inputUUID = jobContext.runStepOnce("step-2", UUID::randomUUID);
        TestMetadata testMetadata = jobContext.runStepOnce("step-3", () -> new TestMetadata("some input"));

        // WHEN
        String jobAsJson = jobMapper.serializeJob(job);
        Job actualJob = jobMapper.deserializeJob(jobAsJson);

        // THEN
        assertThat(actualJob).isEqualTo(job);
        RunnerJobContext actualJobContext = new RunnerJobContext(job);
        assertThat(actualJobContext.runStepOnce("step-1", () -> "result-2")).isEqualTo(inputString);
        assertThat(actualJobContext.runStepOnce("step-2", UUID::randomUUID)).isEqualTo(inputUUID);
        assertThat(actualJobContext.runStepOnce("step-3", () -> new TestMetadata("some other input"))).isEqualTo(testMetadata);
    }

    public static class TestMetadata implements JobContext.Metadata {
        private String input;
        private Instant instant;
        private Path path;
        private File file;

        protected TestMetadata() {
        }

        public TestMetadata(String input) {
            this(input, now(), Paths.get("/tmp"), new File("/tmp"));
        }

        public TestMetadata(String input, Instant instant, Path path, File file) {
            this.input = input;
            this.instant = instant;
            this.path = path;
            this.file = file;
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