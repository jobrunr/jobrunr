package org.jobrunr.utils.mapper.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jobrunr.utils.mapper.jackson.modules.JobRunrModule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonJsonMapperLoadModulesTest {

    @Test
    void testLoadModulesIncludesJobRunrTimeModule() {
        List<Module> modules = ObjectMapper.findModules();
        assertThat(modules)
                .isNotEmpty()
                .hasAtLeastOneElementOfType(JobRunrModule.class);
    }

}