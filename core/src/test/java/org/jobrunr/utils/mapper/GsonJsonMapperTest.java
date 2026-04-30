package org.jobrunr.utils.mapper;

import org.assertj.core.api.Assertions;
import org.jobrunr.jobs.Job;
import org.jobrunr.utils.annotations.Because;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.data.Index.atIndex;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

public class GsonJsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new GsonJsonMapper();
    }

    @Override
    @Test
    @Disabled("No regression introduced for Gson coming from 4.0.0")
    protected void testSerializeAndDeserializeEnqueuedJobComingFrom4Dot0Dot0() {
    }

    @Override
    @Test
    @Disabled("Gson does not know difference between set or list for singleton")
    protected void testCanSerializeSetToCollection() {
    }

    @Override
    @Test
    @Because("Gson does not know type in actual list due to type erasure and changes from Long to Double")
    protected void testCanSerializeListToCollections() {
        Long value = Integer.MAX_VALUE + 2L;
        Job job = anEnqueuedJob().withJobLambda(() -> testService.doWorkWithCollection(List.of(value))).build();

        String jobAsString = jsonMapper.serialize(job);

        Job deserializedJob = jsonMapper.deserialize(jobAsString, Job.class);

        assertThat(deserializedJob.getJobDetails())
                .hasArg(x -> Assertions.assertThat(x.getObject()).isInstanceOf(List.class), atIndex(0));
    }
}
