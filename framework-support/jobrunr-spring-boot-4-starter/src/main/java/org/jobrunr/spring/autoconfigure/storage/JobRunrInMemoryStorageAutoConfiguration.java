package org.jobrunr.spring.autoconfigure.storage;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(JobRunrAutoConfiguration.class)
@ConditionalOnProperty(prefix = "jobrunr.database", name = "type", havingValue = "mem")
public class JobRunrInMemoryStorageAutoConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrInMemoryStorageAutoConfiguration.class);

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider memStorageProvider(JobMapper jobMapper) {
        StorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(jobMapper);
        LOGGER.warn("You're JobRunr running with the {} which is not a persisted storage. Data saved in this storage will be lost on restart.", InMemoryStorageProvider.class.getSimpleName());
        return storageProvider;
    }

}
