package org.jobrunr.utils.mapper.jackson3;

import app.jobrunr.entities.Entity;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.mapper.AbstractJsonMapperTest;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Jackson3JsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        var typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(TestService.Task.class);
        return new Jackson3JsonMapper(typeValidatorBuilder);
    }


    @Test
    void canDeserializeCustomPolymorphicType() {
        var typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Entity.class);
        var jsonMapper = new Jackson3JsonMapper(typeValidatorBuilder);
        var entities = new ArrayList<>(List.of(new Entity("JobRunr")));

        var entitiesAsJsonString = jsonMapper.serialize(entities);
        var deserializedEntities = jsonMapper.deserialize(entitiesAsJsonString, ArrayList.class);

        assertThat(entities).isEqualTo(deserializedEntities);
    }

    @Test
    void canDeserializeJobWithImmutableSubList() {
        var values = List.of("first", "second").subList(0, 1);
        var payload = new ListPayload(values);
        var job = new Job(new JobDetails("Example", null, "execute",
                List.of(new JobParameter(ListPayload.class, payload))));
        var mapper = new JobMapper(newJsonMapper());

        var restored = mapper.deserializeJob(mapper.serializeJob(job));
        var parameter = restored.getJobDetails().getJobParameters().get(0);

        assertThat(parameter.isDeserializable()).isTrue();
        assertThat(parameter.getObject()).isEqualTo(payload);
    }

    public record ListPayload(List<String> values) {
    }

}