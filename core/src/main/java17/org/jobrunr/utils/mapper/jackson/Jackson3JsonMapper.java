package org.jobrunr.utils.mapper.jackson;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jobrunr.jobs.Job;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.modules.JobMixin;
import org.jobrunr.utils.mapper.jackson.modules.JobRunrJackson3Module;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.io.OutputStream;
import java.text.SimpleDateFormat;

public class Jackson3JsonMapper implements JsonMapper {

    private final tools.jackson.databind.json.JsonMapper jsonMapper;

    public Jackson3JsonMapper() {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("org.jobrunr.")
                .allowIfSubTypeIsArray()
                .build();

        this.jsonMapper = tools.jackson.databind.json.JsonMapper.builder()
                .addMixIn(Job.class, JobMixin.class)
                .enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                .enable(EnumFeature.READ_ENUMS_USING_TO_STRING)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .changeDefaultVisibility(vc -> vc
                        .with(JsonAutoDetect.Visibility.NONE)
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                        .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                        .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                )
                .disable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .addModules(new JobRunrJackson3Module())
                .activateDefaultTypingAsProperty(typeValidator, DefaultTyping.OBJECT_AND_NON_CONCRETE, "@class")
                .build();
    }

    @Override
    public String serialize(Object object) {
        return jsonMapper.writeValueAsString(object);
    }

    @Override
    public void serialize(OutputStream outputStream, Object object) {
        jsonMapper.writeValue(outputStream, object);
    }

    @Override
    public <T> T deserialize(String serializedObjectAsString, Class<T> clazz) {
        return jsonMapper.readValue(serializedObjectAsString, clazz);
    }
}
