package org.jobrunr.utils.mapper.jackson3;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.utils.mapper.JobParameterJsonMapperException;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.modules.JobMixin;
import org.jobrunr.utils.mapper.jackson3.modules.JobRunrModule;
import org.jobrunr.utils.mapper.jackson3.modules.JobRunrTimeModule;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.io.OutputStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Jackson3JsonMapper implements JsonMapper {

    private final tools.jackson.databind.json.JsonMapper jsonMapper;

    public Jackson3JsonMapper() {
        this(tools.jackson.databind.json.JsonMapper.builder());
    }

    public Jackson3JsonMapper(tools.jackson.databind.json.JsonMapper.Builder jsonMapperBuilder) {
        this(jsonMapperBuilder, BasicPolymorphicTypeValidator.builder());
    }

    public Jackson3JsonMapper(BasicPolymorphicTypeValidator.Builder typeValidatorBuilder) {
        this(tools.jackson.databind.json.JsonMapper.builder(), typeValidatorBuilder);
    }

    public Jackson3JsonMapper(tools.jackson.databind.json.JsonMapper.Builder jsonMapperBuilder, BasicPolymorphicTypeValidator.Builder typeValidatorBuilder) {
        var typeValidator = typeValidatorBuilder
                .allowIfSubType(JobState.class)
                .allowIfSubType(AbstractJob.class)
                .allowIfSubType(JobContext.Metadata.class)
                .allowIfSubType(CopyOnWriteArrayList.class) // for Job History
                .allowIfSubType(ConcurrentHashMap.class) // for Job Metadata
                .allowIfSubType(Path.class)
                .build();

        this.jsonMapper = jsonMapperBuilder
                .addMixIn(Job.class, JobMixin.class)
                .enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                .enable(EnumFeature.READ_ENUMS_USING_TO_STRING)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
                .defaultTimeZone(TimeZone.getDefault())
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .changeDefaultVisibility(vc -> vc
                        .with(JsonAutoDetect.Visibility.NONE)
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                        .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                )
                .disable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .addModules(new JobRunrModule(), new JobRunrTimeModule())
                .activateDefaultTypingAsProperty(typeValidator, DefaultTyping.OBJECT_AND_NON_CONCRETE, "@class")
                .build();
    }

    @Override
    public String serialize(Object object) {
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (InvalidDefinitionException e) {
            throw new JobParameterJsonMapperException("The job parameters are not serializable.", e);
        }
    }

    @Override
    public void serialize(OutputStream outputStream, Object object) {
        jsonMapper.writeValue(outputStream, object);
    }

    @Override
    public <T> T deserialize(String serializedObjectAsString, Class<T> clazz) {
        try {
            return jsonMapper.readValue(serializedObjectAsString, clazz);
        } catch (InvalidDefinitionException e) {
            throw JobRunrException.configurationException("Did you register all necessary Jackson Modules?", e);
        }
    }
}
