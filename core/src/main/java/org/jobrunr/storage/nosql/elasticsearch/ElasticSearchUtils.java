package org.jobrunr.storage.nosql.elasticsearch;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
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

public class ElasticSearchUtils {

    public static CreateIndexRequest jobIndex() {
        return new CreateIndexRequest(jobIndexName())
                .mapping(mapping(
                        (sb, map) -> {
                            sb.append(Jobs.FIELD_JOB_AS_JSON);
                            map.put("type", "text");
                            map.put("index", false);
                            map.put("store", true);
                        },
                        (sb, map) -> {
                            sb.append(Jobs.FIELD_STATE);
                            map.put("type", "keyword");
                        },
                        (sb, map) -> {
                            sb.append(Jobs.FIELD_JOB_SIGNATURE);
                            map.put("type", "keyword");
                        },
                        (sb, map) -> {
                            sb.append(Jobs.FIELD_SCHEDULED_AT);
                            map.put("type", "date_nanos");
                        },
                        (sb, map) -> {
                            sb.append(Jobs.FIELD_UPDATED_AT);
                            map.put("type", "date_nanos");
                        }
                ));
    }

    public static CreateIndexRequest recurringJobIndex() {
        return new CreateIndexRequest(recurringJobIndexName())
                .mapping(mapping(
                        (sb, map) -> {
                            sb.append(RecurringJobs.FIELD_JOB_AS_JSON);
                            map.put("type", "text");
                            map.put("index", false);
                            map.put("store", true);
                        }
                ));
    }

    public static CreateIndexRequest backgroundJobServersIndex() {
        return new CreateIndexRequest(backgroundJobServerIndexName())
                .mapping(mapping(
                        (sb, map) -> {
                            sb.append(BackgroundJobServers.FIELD_FIRST_HEARTBEAT);
                            map.put("type", "date_nanos");
                        },
                        (sb, map) -> {
                            sb.append(BackgroundJobServers.FIELD_LAST_HEARTBEAT);
                            map.put("type", "date_nanos");
                        }
                ));
    }

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
}
