package org.jobrunr.autoconfigure.json;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Bean;

import javax.json.bind.Jsonb;

@ConditionalOnClass(Jsonb.class)
@ConditionalOnResource(resources = {"classpath:META-INF/services/javax.json.bind.spi.JsonbProvider",
        "classpath:META-INF/services/javax.json.spi.JsonProvider"})
public class JobRunrJsonbAutoConfiguration {

    @Bean(name = "jsonMapper")
    @ConditionalOnMissingBean
    public JsonMapper jsonbJsonMapper() {
        return new JsonbJsonMapper();
    }
}
