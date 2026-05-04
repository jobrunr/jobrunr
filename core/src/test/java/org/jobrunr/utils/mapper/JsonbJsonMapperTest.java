package org.jobrunr.utils.mapper;

import org.jobrunr.jobs.Job;
import org.jobrunr.utils.annotations.Because;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

public class JsonbJsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new JsonbJsonMapper();
    }

    @Override
    @Test
    @Disabled("I don't understand: custom deserializer is registered but get following exception: Unable to make field private final java.lang.String java.io.File.path accessible: module java.base does not \"opens java.io\" to unnamed module")
    void testSerializeAndDeserializeEnqueuedJobWithFileJobParameter() {
    }

    @Override
    @Test
    @Disabled("No regression introduced for JsonB coming from 4.0.0")
    protected void testSerializeAndDeserializeEnqueuedJobComingFrom4Dot0Dot0() {
    }

    @Override
    @Test
    @Disabled("JsonB does not know difference between set or list for singleton")
    protected void testCanSerializeSetToCollection() {
    }

    @Override
    @Test
    @Because("JsonB does not know type in actual list due to type erasure and changes from Long to Double")
    protected void testCanSerializeListToCollections() {
        Long value = Integer.MAX_VALUE + 2L;
        Job job = anEnqueuedJob().withJobLambda(() -> testService.doWorkWithCollection(List.of(value))).build();

        String jobAsString = jsonMapper.serialize(job);

        Job deserializedJob = jsonMapper.deserialize(jobAsString, Job.class);

        assertThat(deserializedJob.getJobDetails())
                .hasArg(x -> assertThat(x.getObject()).isInstanceOf(List.class), atIndex(0));
    }
}