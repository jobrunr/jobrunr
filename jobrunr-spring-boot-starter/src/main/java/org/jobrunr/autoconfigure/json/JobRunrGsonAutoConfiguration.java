package org.jobrunr.autoconfigure.json;


import com.google.gson.Gson;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@ConditionalOnClass(Gson.class)
public class JobRunrGsonAutoConfiguration {

    @Bean(name = "jsonMapper")
    @ConditionalOnMissingBean
    public JsonMapper gsonJsonMapper() {
        return new GsonJsonMapper();
    }
}
