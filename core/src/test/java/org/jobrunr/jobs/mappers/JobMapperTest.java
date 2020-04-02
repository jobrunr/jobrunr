package org.jobrunr.jobs.mappers;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;

class JobMapperTest {

    private JobMapper jobMapper;

    @BeforeEach
    void setUp() {
        jobMapper = new JobMapper(new JacksonJsonMapper());
    }

    @Test
    void testSerializeAndDeserializeJob() {
        Job job = anEnqueuedJob().build();

        String jobAsString = jobMapper.serializeJob(job);
        final Job actualJob = jobMapper.deserializeJob(jobAsString);

        assertThat(actualJob).usingRecursiveComparison().isEqualTo(job);
    }

    @Test
    void serializeAndDeserializeRecurringJob() {
        RecurringJob recurringJob = aDefaultRecurringJob().build();

        String jobAsString = jobMapper.serializeRecurringJob(recurringJob);
        final RecurringJob actualRecurringJob = jobMapper.deserializeRecurringJob(jobAsString);

        assertThat(actualRecurringJob).usingRecursiveComparison().isEqualTo(recurringJob);
    }
}