package org.jobrunr.autoconfigure.storage;

import com.mongodb.client.MongoClient;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(MongoClient.class)
@AutoConfigureAfter(MongoAutoConfiguration.class)
public class JobRunrMongoDBStorageAutoConfiguration {

    @Bean(name = "storageProvider")
    @ConditionalOnMissingBean
    public StorageProvider mongoDBStorageProvider(MongoClient mongoClient) {
        return new MongoDBStorageProvider(mongoClient);
    }
}
