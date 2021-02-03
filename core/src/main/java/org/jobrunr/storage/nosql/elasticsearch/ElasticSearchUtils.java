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
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.jobrunr.storage.StorageProviderUtils.Metadata;

public class ElasticSearchUtils {


    public static IndexRequest jobStatsIndex() {
        try {
            XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint();
            builder.startObject();
            builder.field(StateName.AWAITING.name(), 0L);
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
        return "jobrunr_" + Jobs.NAME;
    }

    public static String recurringJobIndexName() {
        return "jobrunr_" + RecurringJobs.NAME;
    }

    public static String metadataIndexName() {
        return "jobrunr_" + Metadata.NAME;
    }

    public static String backgroundJobServerIndexName() {
        return "jobrunr_" + BackgroundJobServers.NAME;
    }

    private static Map<String, Object> mapping(BiConsumer<StringBuilder, Map<String, Object>>... consumers) {
        Map<String, Object> jsonMap = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        jsonMap.put("properties", properties);

        for (BiConsumer<StringBuilder, Map<String, Object>> consumer : consumers) {
            StringBuilder sb = new StringBuilder();
            Map<String, Object> fieldProperties = new HashMap<>();
            consumer.accept(sb, fieldProperties);
            properties.put(sb.toString(), fieldProperties);

        }
        return jsonMap;
    }

    public static void sleep(long amount) {
        try {
            Thread.sleep(amount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
