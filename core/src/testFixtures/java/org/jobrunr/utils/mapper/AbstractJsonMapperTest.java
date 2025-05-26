package org.jobrunr.utils.mapper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobDashboardProgressBar;
import org.jobrunr.jobs.exceptions.JobParameterNotDeserializableException;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.server.runner.RunnerJobContext;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.TestService.Task;
import org.jobrunr.utils.annotations.Because;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThatJson;
import static org.jobrunr.JobRunrAssertions.contentOfResource;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;

public abstract class AbstractJsonMapperTest {

    protected JsonMapper jsonMapper;
    private TestService testService;

    @BeforeEach
    void setUp() {
        jsonMapper = newJsonMapper();
        testService = new TestService();
    }

    public abstract JsonMapper newJsonMapper();

    @Test
    void testSerializeAndDeserializeEnqueuedJob() {
        Job job = anEnqueuedJob().build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/enqueued-job.json"));

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeEnqueuedJobWithCustomObject() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWork(new TestService.Work(3, "a String", UUID.randomUUID())))
                .build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/enqueued-job-custom-object-parameter.json"));

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeWithJobContext() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWork(5, JobContext.Null))
                .build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/enqueued-job-null-jobcontext.json"));

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeJobInProgressWithLoggingAndProgressBar() {
        Job job = aJobInProgress().build();
        final RunnerJobContext jobContext = new RunnerJobContext(job);

        jobContext.logger().info("this is an info message");
        jobContext.logger().warn("this is a warning message");
        jobContext.logger().error("this is an error message");

        final JobDashboardProgressBar progressBar = jobContext.progressBar(80);
        progressBar.setProgress(10);

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/job-in-progress-with-progressbar-and-logging.json"));

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeEnqueuedJobWithInstantJobParameter() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWork(UUID.randomUUID(), 3, now()))
                .build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/enqueued-job-instant-parameter.json"));

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeEnqueuedJobWithLocalDateTimeJobParameter() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWork(LocalDateTime.now()))
                .build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/enqueued-job-localdatetime-parameter.json"));

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeEnqueuedJobWithOffsetDateTimeJobParameter() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWork(OffsetDateTime.now()))
                .build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/enqueued-job-offsetdatetime-parameter.json"));

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeEnqueuedJobWithPathJobParameter() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWorkWithPath(Paths.get("/tmp", "jobrunr", "file.xml")))
                .build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/enqueued-job-path-parameter.json"));

        Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeEnqueuedJobWithFileJobParameter() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWorkWithFile(new File("/tmp/test.txt")))
                .build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/enqueued-job-file-parameter.json"));

        Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeEnqueuedJobWithInterfaceAsJobParameter() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWorkWithCommand(new TestService.SimpleCommand("Hello", 5)))
                .build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/enqueued-job-interface-object-parameter.json"));

        Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeSucceededJobWithJobParameterThatDoesNotExistAnymore() {
        final String jobAsString = contentOfResource("/org/jobrunr/utils/mapper/succeeded-job-with-job-parameter-that-does-not-exist.json");

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isNotNull();
        assertThat(actualJob.getJobDetails())
                .hasArgSize(1)
                .hasArgComparingStringFormat("{\"now\":\"2020-01-01T00:00:00Z\",\"someId\":\"072da403-f87b-4529-8af2-0c78c395ec5f\",\"someList\":[\"a\",\"b\",\"c\"],\"someString\":\"some string\"}")
                .hasNotDeserializableExceptionEqualTo(new JobParameterNotDeserializableException("org.jobrunr.utils.mapper.AbstractJsonMapperTest$SomeCustomObjectThatDoesNotExist", "java.lang.IllegalArgumentException", "Class not found: org.jobrunr.utils.mapper.AbstractJsonMapperTest$SomeCustomObjectThatDoesNotExist"));
    }

    @Test
    void testSerializeAndDeserializeSucceededJob() {
        Job job = aSucceededJob().build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/succeeded-job.json"));

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeRecurringJob() {
        RecurringJob recurringJob = aDefaultRecurringJob().withName("some name").build();

        final String recurringJobAsString = jsonMapper.serialize(recurringJob);
        assertThatJson(recurringJobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/recurring-job.json"));

        final RecurringJob actualRecurringJob = jsonMapper.deserialize(recurringJobAsString, RecurringJob.class);
        assertThat(actualRecurringJob).isEqualTo(recurringJob);
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/254")
    void testSerializeAndDeserializeEnqueuedJobAfter4Dot0Dot0() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWork(1L))
                .build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/enqueued-job-github-254.json"));
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/254")
    protected void testSerializeAndDeserializeEnqueuedJobComingFrom4Dot0Dot0() {
        // jobs created in 4.0.1
        Job job = aJob()
                .withId(UUID.fromString("8bf98a10-f673-4fd8-9b9c-43ded0030910"))
                .withName("an enqueued job")
                .withState(new EnqueuedState(), Instant.parse("2021-11-10T11:37:40.551537Z"))
                .withJobDetails(() -> testService.doWork(1L))
                .build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/enqueued-job-github-254.json"));

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);

        // jobs created in 4.0.0
        final String jobAsStringFrom4Dot0Dot0 = contentOfResource("/org/jobrunr/utils/mapper/enqueued-job-github-254-input.json");

        final Job actualJobFrom4Dot0Dot0 = jsonMapper.deserialize(jobAsStringFrom4Dot0Dot0, Job.class);
        assertThat(actualJobFrom4Dot0Dot0).isEqualTo(job, "locker", "labels", "newState");
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/536")
    void testSerializeAndDeserializeJobsComingFrom4Dot0Dot0() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        Job job = aJob()
                .withId(UUID.fromString("8bf98a10-f673-4fd8-9b9c-43ded0030910"))
                .withName("an enqueued job")
                .withState(new EnqueuedState(), Instant.parse("2021-11-10T11:37:40.551537Z"))
                .withJobDetails(() -> testService.doWork(xValues, yValues))
                .build();

        final String jobAsString = jsonMapper.serialize(job);
        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/536")
    void testSerializeAndDeserializeRecurringJobsComingFrom5() {
        // recurring jobs created in 5.0.0
        final String recurringJobAsStringFrom5 = contentOfResource("/org/jobrunr/utils/mapper/existing-recurring-job-github-714-input.json");

        final RecurringJob actualRecurringJob = jsonMapper.deserialize(recurringJobAsStringFrom5, RecurringJob.class);
        assertThat(actualRecurringJob)
                .isNotNull()
                .hasLabels(emptyList());
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/536")
    void testSerializeAndDeserializeJobsComingFrom5() {
        // jobs created in 5.0.0
        final String enqueuedJobAsStringFrom5 = contentOfResource("/org/jobrunr/utils/mapper/existing-enqueued-job-github-714-input.json");

        final Job actualJob = jsonMapper.deserialize(enqueuedJobAsStringFrom5, Job.class);
        assertThat(actualJob)
                .isNotNull()
                .hasLabels(emptyList());
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/282")
    void testCanSerializeCollections() {
        Long value = Integer.MAX_VALUE + 2L;
        Job job = anEnqueuedJob().withJobDetails(() -> testService.doWorkWithCollection(singleton(value))).build();

        String jobAsString = jsonMapper.serialize(job);

        Job deserializedJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(deserializedJob.getJobDetails())
                .hasArgs(singleton(value));
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/375")
    void testCanSerializeEnums() {
        Job job = anEnqueuedJob().withJobDetails(() -> testService.doWorkWithEnum(Task.PROGRAMMING)).build();

        String jobAsString = jsonMapper.serialize(job);

        Job deserializedJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(deserializedJob.getJobDetails())
                .hasArgs(Task.PROGRAMMING);
    }

    @Test
    void testCanSerializeAndDeserializeWithAllFieldsNotNull() {
        Job job = anEnqueuedJob()
                .withAmountOfRetries(6)
                .withRecurringJobId("my-recurring-job")
                .withJobDetails(() -> testService.doWorkWithEnum(Task.PROGRAMMING))
                .withProcessingState()
                .withFailedState()
                .withScheduledState()
                .withEnqueuedState(Instant.now())
                .withProcessingState()
                .withSucceededState()
                .build();

        String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/complete-job.json"));

        Job deserializedJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(deserializedJob.getJobDetails())
                .hasArgs(Task.PROGRAMMING);
    }

    @Test
    void testCanDeserializeFromV6WithSomeFieldsNull() {
        String serializedJobInV6 = contentOfResource("/org/jobrunr/utils/mapper/complete-job-v6.0.json");

        assertThatCode(() -> jsonMapper.deserialize(serializedJobInV6, Job.class)).doesNotThrowAnyException();
    }
}
