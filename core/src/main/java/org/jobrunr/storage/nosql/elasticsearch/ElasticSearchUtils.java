package org.jobrunr.storage.nosql.elasticsearch;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;

import java.io.IOException;

import static org.jobrunr.storage.StorageProviderUtils.Metadata;

public class ElasticSearchUtils {

    private ElasticSearchUtils() {
    }

    public static final String JOBRUNR_PREFIX = "jobrunr_";

    public static IndexRequest jobStatsIndex() {
        try {
            XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint();
            builder.startObject();
            builder.field(StateName.SCHEDULED.name(), 0L);
            builder.field(StateName.ENQUEUED.name(), 0L);
            builder.field(StateName.PROCESSING.name(), 0L);
            builder.field(StateName.SUCCEEDED.name(), 0L);
            builder.field(StateName.FAILED.name(), 0L);
            builder.field(StateName.DELETED.name(), 0L);
            builder.endObject();
            return new IndexRequest("jobrunr_job_stats")
                    .id("job_stats")
                    .source(builder);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    public static String jobIndexName() {
        return JOBRUNR_PREFIX + Jobs.NAME;
    }

    public static String recurringJobIndexName() {
        return JOBRUNR_PREFIX + RecurringJobs.NAME;
    }

    public static String metadataIndexName() {
        return JOBRUNR_PREFIX + Metadata.NAME;
    }

    public static String backgroundJobServerIndexName() {
        return JOBRUNR_PREFIX + BackgroundJobServers.NAME;
    }

    public static void sleep(long amount) {
        try {
            Thread.sleep(amount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
