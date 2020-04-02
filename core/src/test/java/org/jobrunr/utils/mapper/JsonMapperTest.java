package org.jobrunr.utils.mapper;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThatJson;
import static org.jobrunr.JobRunrAssertions.contentOfResource;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

public abstract class JsonMapperTest {

    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = newJsonMapper();
    }

    public abstract JsonMapper newJsonMapper();

    @Test
    void testSerializeAndDeserialize() {
        Job job = anEnqueuedJob().build();

        final String jobAsString = jsonMapper.serialize(job);
        assertThatJson(jobAsString).isEqualTo(contentOfResource("/org/jobrunr/utils/mapper/job.json"));

        final Job actualJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(actualJob).isEqualTo(job);
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
