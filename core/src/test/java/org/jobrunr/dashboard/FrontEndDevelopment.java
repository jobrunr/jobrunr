package org.jobrunr.dashboard;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StubDataProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

import java.time.Instant;
import java.util.Arrays;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.jobrunr.jobs.JobDetailsTestBuilder.classThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aJob;

/**
 * Main Class to run for FrontEndDevelopment
 */
public class FrontEndDevelopment {

    public static void main(String[] args) throws InterruptedException {
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
                MongoClientSettings.getDefaultCodecRegistry()
        );
        MongoClient mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress("127.0.0.1", 27017))))
                        .codecRegistry(codecRegistry)
                        .build());

        StorageProvider storageProvider = new MongoDBStorageProvider(mongoClient);
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));

        StubDataProvider.using(storageProvider)
                //.addALotOfEnqueuedJobsThatTakeSomeTime()
                .addSomeRecurringJobs();

        storageProvider.save(aJob().withJobDetails(classThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(1, DAYS))).build());
        storageProvider.save(aJob().withJobDetails(methodThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(1, DAYS))).build());

        JobRunr
                .configure()
                .useStorageProvider(storageProvider)
                .useDashboardIf(dashboardIsEnabled(args), 8000)
                .useDefaultBackgroundJobServer()
                .initialize();

        //BackgroundJob.<TestService>enqueue(x -> x.doWorkThatTakesLong(JobContext.Null));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> Thread.currentThread().interrupt()));

        Thread.currentThread().join();
    }

    private static boolean dashboardIsEnabled(String[] args) {
        return !argsContains(args, "dashboard=false");
    }

    private static boolean argsContains(String[] args, String argToSearch) {
        if (args.length == 0) return false;
        for (String arg : args) {
            if (argToSearch.equalsIgnoreCase(arg)) return true;
        }
        return false;
    }
}
