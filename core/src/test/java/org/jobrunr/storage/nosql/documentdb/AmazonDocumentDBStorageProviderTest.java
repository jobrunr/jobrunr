package org.jobrunr.storage.nosql.documentdb;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Disabled;

import java.util.Arrays;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

@Disabled("Can only be run when having an SSH connection an Amazon EC2 instance which tunnels all requests to the DocumentDB cluster")
public class AmazonDocumentDBStorageProviderTest extends StorageProviderTest {

    private static MongoClient mongoClient;

    @Override
    protected void cleanup() {
        MongoDatabase jobrunrDb = mongoClient().getDatabase(MongoDBStorageProvider.DEFAULT_DB_NAME);
        jobrunrDb.getCollection(StorageProviderUtils.Jobs.NAME).deleteMany(new Document());
        jobrunrDb.getCollection(StorageProviderUtils.RecurringJobs.NAME).deleteMany(new Document());
        jobrunrDb.getCollection(StorageProviderUtils.BackgroundJobServers.NAME).deleteMany(new Document());
        jobrunrDb.getCollection(StorageProviderUtils.JobStats.NAME).deleteMany(new Document());
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final MongoDBStorageProvider dbStorageProvider = new AmazonDocumentDBStorageProvider(mongoClient(), rateLimit().withoutLimits());
        dbStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return dbStorageProvider;
    }

    private MongoClient mongoClient() {
        String username = "jobrunruser";
        String password = "jobrunruser";

        String clusterEndpoint = "127.0.0.1:27017";

        ServerAddress serverAddress = new ServerAddress(clusterEndpoint);
        MongoCredential credential = MongoCredential.createCredential(username, "jobrunr", password.toCharArray());

        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
                MongoClientSettings.getDefaultCodecRegistry()
        );
        if (mongoClient == null) {
            mongoClient = MongoClients.create(
                    MongoClientSettings.builder()
                            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(serverAddress)))
                            .credential(credential)
                            .codecRegistry(codecRegistry)
                            .build());

        }
        return mongoClient;
    }
}
