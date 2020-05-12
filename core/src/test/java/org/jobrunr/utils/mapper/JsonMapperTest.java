package org.jobrunr.utils.mapper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.Instant.now;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThatJson;
import static org.jobrunr.JobRunrAssertions.contentOfResource;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;

public abstract class JsonMapperTest {

    private JsonMapper jsonMapper;
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
    void testSerializeAndDeserializeSucceededJob() {
        Job job = aSucceededJob().build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/succeeded-job.json"));

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeRecurringJob() {
        RecurringJob recurringJob = aDefaultRecurringJob().build();

        final String recurringJobAsString = jsonMapper.serialize(recurringJob);
        assertThatJson(recurringJobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/recurring-job.json"));

        final RecurringJob actualRecurringJob = jsonMapper.deserialize(recurringJobAsString, RecurringJob.class);
        assertThat(actualRecurringJob).isEqualTo(actualRecurringJob);
    }

    @Test
    void testSerializeToWriter() {
        StringWriter stringWriter = new StringWriter();

        Job job = anEnqueuedJob().build();

        jsonMapper.serialize(stringWriter, job);
        final String jobAsString = stringWriter.toString();
        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);

        assertThat(actualJob).isEqualTo(job);
    }

}
