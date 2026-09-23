package org.jobrunr.mcp.spring;

import org.jobrunr.mcp.JobRunrMcpConnector;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JobRunrMcpAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JobRunrMcpAutoConfiguration.class))
            .withBean(StorageProvider.class, InMemoryStorageProvider::new)
            .withBean(JsonMapper.class, JacksonJsonMapper::new);

    @Test
    void connectsOnlyWhenAConnectorTokenIsConfigured() {
        contextRunner
                .withPropertyValues("jobrunr.dashboard.mcp.token=jrc_read_token", "jobrunr.dashboard.mcp.url=http://127.0.0.1:1")
                .run(context -> assertThat(context).hasSingleBean(JobRunrMcpConnector.class));
        contextRunner
                .withPropertyValues("jobrunr.dashboard.mcp.token=")
                .run(context -> assertThat(context).doesNotHaveBean(JobRunrMcpConnector.class));
    }
}
