package org.jobrunr.spring.autoconfigure.storage;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;

@Configuration
@AutoConfigureBefore(JobRunrAutoConfiguration.class)
@ConditionalOnProperty(prefix = "org.jobrunr.database", name = "type", havingValue = "mem", matchIfMissing = false)
public class JobRunrInMemoryStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider memStorageProvider(BeanFactory beanFactory, JobMapper jobMapper, JobRunrProperties properties) {
        StorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

}
