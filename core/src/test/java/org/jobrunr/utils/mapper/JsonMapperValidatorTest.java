package org.jobrunr.utils.mapper;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.junit.jupiter.api.Test;

import javax.json.bind.JsonbConfig;
import java.text.SimpleDateFormat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.utils.mapper.JsonMapperValidator.validateJsonMapper;

public class JsonMapperValidatorTest {

    @Test
    void testInvalidJacksonJsonMapperNoJavaTimeModule() {
        assertThatThrownBy(() -> validateJsonMapper(new InvalidJacksonJsonMapper(new ObjectMapper())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The JsonMapper you provided cannot be used as it deserializes jobs in an incorrect way.");
        //.hasRootCauseMessage("Java 8 date/time type `java.time.Instant` not supported by default: add Module \"com.fasterxml.jackson.datatype:jackson-datatype-jsr310\" to enable handling (through reference chain: org.jobrunr.jobs.Job[\"jobStates\"]->java.util.Collections$UnmodifiableRandomAccessList[0]->org.jobrunr.jobs.states.ProcessingState[\"createdAt\"])");
    }

    @Test
    void testInvalidJacksonJsonMapperNoISO8601TimeFormat() {
        assertThatThrownBy(() -> validateJsonMapper(new InvalidJacksonJsonMapper(new ObjectMapper().registerModule(new JavaTimeModule()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The JsonMapper you provided cannot be used as it deserializes jobs in an incorrect way.")
                .hasRootCauseMessage("Timestamps are wrongly formatted for JobRunr. They should be in ISO8601 format.");
    }

    @Test
    void testInvalidJacksonJsonMapperPropertiesInsteadOfFields() {
        assertThatThrownBy(() -> validateJsonMapper(new InvalidJacksonJsonMapper(new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
                ))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The JsonMapper you provided cannot be used as it deserializes jobs in an incorrect way.")
                .hasRootCauseMessage("Job Serialization should use fields and not getters/setters.");
    }


    @Test
    void testInvalidJacksonJsonMapperNoPolymorphism() {
        assertThatThrownBy(() -> validateJsonMapper(new InvalidJacksonJsonMapper(new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
                        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                ))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The JsonMapper you provided cannot be used as it deserializes jobs in an incorrect way.")
                .hasRootCauseMessage("Polymorphism is not supported as no @class annotation is present with fully qualified name of the different Job states.");
    }

    @Test
    void testInvalidGsonJsonMapper() {
        assertThatThrownBy(() -> validateJsonMapper(new InvalidGsonJsonMapper(new GsonBuilder().create()
                ))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The JsonMapper you provided cannot be used as it deserializes jobs in an incorrect way.")
                .hasRootCauseMessage("Timestamps are wrongly formatted for JobRunr. They should be in ISO8601 format.");
    }

    @Test
    void testInvalidJsonbJsonMapper() {
        assertThatThrownBy(() -> validateJsonMapper(new InvalidJsonbJsonMapper(new JsonbConfig()))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The JsonMapper you provided cannot be used as it deserializes jobs in an incorrect way.");
    }

    @Test
    void testValidJacksonJsonMapper() {
        assertThatCode(() -> validateJsonMapper(new JacksonJsonMapper())).doesNotThrowAnyException();
    }

    @Test
    void testValidGsonJsonMapper() {
        assertThatCode(() -> validateJsonMapper(new GsonJsonMapper())).doesNotThrowAnyException();
    }

    @Test
    void testValidJsonBJsonMapper() {
        assertThatCode(() -> validateJsonMapper(new JsonbJsonMapper())).doesNotThrowAnyException();
    }

    public class InvalidJacksonJsonMapper extends JacksonJsonMapper {

        public InvalidJacksonJsonMapper(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        @Override
        protected ObjectMapper initObjectMapper(ObjectMapper objectMapper, boolean moduleAutoDiscover) {
            return objectMapper;
        }
    }

    public class InvalidGsonJsonMapper extends GsonJsonMapper {

        public InvalidGsonJsonMapper(Gson gson) {
            super(gson);
        }

        @Override
        protected Gson initGson(GsonBuilder gsonBuilder) {
            return gsonBuilder.create();
        }
    }

    public class InvalidJsonbJsonMapper extends JsonbJsonMapper {

        public InvalidJsonbJsonMapper(JsonbConfig config) {
            super(config);
        }

        @Override
        protected JsonbConfig initJsonbConfig(JsonbConfig jsonbConfig) {
            return jsonbConfig;
        }
    }
}
