package org.jobrunr.utils.mapper.jackson;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jobrunr.jobs.Job;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.modules.JobMixin;
import org.jobrunr.utils.mapper.jackson.modules.JobRunrJackson3Module;
import tools.jackson.databind.cfg.EnumFeature;

import java.io.OutputStream;
import java.text.SimpleDateFormat;

public class Jackson3JsonMapper implements JsonMapper {

    private final tools.jackson.databind.json.JsonMapper jsonMapper;

    public Jackson3JsonMapper() {
        this.jsonMapper = tools.jackson.databind.json.JsonMapper.builder()
                .addMixIn(Job.class, JobMixin.class)
                .enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                .enable(EnumFeature.READ_ENUMS_USING_TO_STRING)
                .disable(tools.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .changeDefaultVisibility(vc -> vc
                                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                                .withCreatorVisibility(JsonAutoDetect.Visibility.DEFAULT)
                        //vc.withIsGetterVisibility(JsonAutoDetect.Visibility.ANY); --- TODO how to do .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE) ??
                )
                .addModules(new JobRunrJackson3Module())    // TODO logic for auto discovery etc
                //.regi

                //.registerModules(findModules(moduleAutoDiscover))
//                .activateDefaultTypingAsProperty(LaissezFaireSubTypeValidator.instance,
//                        ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS,
//                        "@class")
                .build();

        //jsonMapper(Object.class)
///                .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

//        jsonMapper.config(
//                VisibilityChecker.Std.defaultInstance()
//                        .with(JsonAutoDetect.Visibility.NONE)                     // start hidden
//                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)       // allow fields
//                        .withCreatorVisibility(JsonAutoDetect.Visibility.DEFAULT) // keep default constructor visibility
        //);


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
