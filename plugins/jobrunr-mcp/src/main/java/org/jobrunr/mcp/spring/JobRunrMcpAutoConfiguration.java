package org.jobrunr.mcp.spring;

import org.jobrunr.mcp.JobRunrMcpConnector;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;

/**
 * A Spring Boot AutoConfiguration that connects JobRunr to the JobRunr MCP server once {@code jobrunr.dashboard.mcp.token} holds
 * a connector token. Works for Spring Boot 3 and 4.
 */
@AutoConfiguration
@ConditionalOnExpression("'${jobrunr.dashboard.mcp.token:}' != ''")
public class JobRunrMcpAutoConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public JobRunrMcpConnector jobRunrMcpConnector(@Value("${jobrunr.dashboard.mcp.token}") String token,
                                                   @Value("${jobrunr.dashboard.mcp.url:" + JobRunrMcpConnector.DEFAULT_HUB_URL + "}") String hubUrl,
                                                   StorageProvider storageProvider, JsonMapper jobRunrJsonMapper) {
        return new JobRunrMcpConnector(token, hubUrl, storageProvider, jobRunrJsonMapper);
    }
}
