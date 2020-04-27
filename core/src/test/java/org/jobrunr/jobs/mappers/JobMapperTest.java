package org.jobrunr.jobs.mappers;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;

abstract class JobMapperTest {

    private JobMapper jobMapper;
    private TestService testService;

    @BeforeEach
    void setUp() {
        jobMapper = new JobMapper(getJsonMapper());
        testService = new TestService();
    }

    protected abstract JsonMapper getJsonMapper();

    @Test
    void testSerializeAndDeserializeJob() {
        Job job = anEnqueuedJob().build();

        String jobAsString = jobMapper.serializeJob(job);
        final Job actualJob = jobMapper.deserializeJob(jobAsString);

        assertThat(actualJob).usingRecursiveComparison().isEqualTo(job);
    }

    @Test
    void testSerializeAndDeserializeJobWithPath() {
        Job job = anEnqueuedJob().withJobDetails(() -> testService.doWorkWithPath(Path.of("/tmp", "jobrunr", "log.txt"))).build();

        String jobAsString = jobMapper.serializeJob(job);

        assertThatCode(() -> jobMapper.deserializeJob(jobAsString)).doesNotThrowAnyException();
    }

    @Test
    void serializeAndDeserializeRecurringJob() {
        RecurringJob recurringJob = aDefaultRecurringJob().build();

        String jobAsString = jobMapper.serializeRecurringJob(recurringJob);
        final RecurringJob actualRecurringJob = jobMapper.deserializeRecurringJob(jobAsString);

        assertThat(actualRecurringJob).usingRecursiveComparison().isEqualTo(recurringJob);
    }
}