package org.jobrunr.autoconfigure.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@ConditionalOnClass(ObjectMapper.class)
public class JobRunrJacksonAutoConfiguration {

    @Bean(name = "jsonMapper")
    @ConditionalOnMissingBean
    public JsonMapper jacksonJsonMapper() {
        return new JacksonJsonMapper();
    }

}
