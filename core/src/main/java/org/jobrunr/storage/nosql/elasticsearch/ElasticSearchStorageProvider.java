package org.jobrunr.storage.nosql.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.*;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;
import org.jobrunr.utils.JobUtils;
import org.jobrunr.utils.resilience.RateLimiter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.jobrunr.storage.StorageProviderUtils.areNewJobs;
import static org.jobrunr.storage.StorageProviderUtils.notAllJobsAreExisting;
import static org.jobrunr.storage.StorageProviderUtils.notAllJobsAreNew;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.backgroundJobServerIndexName;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.backgroundJobServersIndex;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.jobIndex;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.jobIndexName;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.jobStatsIndex;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.recurringJobIndex;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.recurringJobIndexName;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class ElasticSearchStorageProvider extends AbstractStorageProvider {

    private final RestHighLevelClient client;
    private ElasticSearchDocumentMapper elasticSearchDocumentMapper;

    public ElasticSearchStorageProvider(String hostName, int port) {
        this(new HttpHost(hostName, port, "http"));
    }

    public ElasticSearchStorageProvider(HttpHost httpHost) {
        this(new RestHighLevelClient(RestClient.builder(httpHost)));
    }

    public ElasticSearchStorageProvider(RestHighLevelClient client) {
        this(client, rateLimit().at1Request().per(SECOND));
    }

    public ElasticSearchStorageProvider(RestHighLevelClient client, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        this.client = client;

        createIndicesIfNecessary();
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.elasticSearchDocumentMapper = new ElasticSearchDocumentMapper(jobMapper);
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        try {
            IndexRequest request = new IndexRequest(backgroundJobServerIndexName())
                    .id(serverStatus.getId().toString())
                    .setRefreshPolicy(IMMEDIATE)
                    .source(elasticSearchDocumentMapper.toXContentBuilderForInsert(serverStatus));
            client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        try {
            UpdateRequest updateRequest = new UpdateRequest(backgroundJobServerIndexName(), serverStatus.getId().toString())
                    .fetchSource(true)
                    .doc(elasticSearchDocumentMapper.toXContentBuilderForUpdate(serverStatus));
            UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
            return cast(updateResponse.getGetResult().getSource().getOrDefault(BackgroundJobServers.FIELD_IS_RUNNING, false));
        } catch (ElasticsearchStatusException e) {
            if (e.status().getStatus() == 404) {
                throw new ServerTimedOutException(serverStatus, new StorageException(e));
            }
            throw e;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        try {
            DeleteRequest deleteRequest = new DeleteRequest(backgroundJobServerIndexName(), serverStatus.getId().toString());
            client.delete(deleteRequest.setRefreshPolicy(IMMEDIATE), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        try {
            SearchRequest searchRequest = new SearchRequest(backgroundJobServerIndexName());
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            searchSourceBuilder.fetchSource(true);
            searchSourceBuilder.sort(BackgroundJobServers.FIELD_FIRST_HEARTBEAT, SortOrder.ASC);
            searchRequest.source(searchSourceBuilder);
            SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);

            return Stream.of(search.getHits().getHits())
                    .map(elasticSearchDocumentMapper::toBackgroundJobServerStatus)
                    .collect(toList());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        try {
            DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(backgroundJobServerIndexName());
            deleteByQueryRequest.setQuery(rangeQuery(BackgroundJobServers.FIELD_LAST_HEARTBEAT).to(heartbeatOlderThan));
            BulkByScrollResponse bulkByScrollResponse = client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
            int amountDeleted = (int) bulkByScrollResponse.getDeleted();
            if (amountDeleted > 0) {
                RefreshRequest request = new RefreshRequest(backgroundJobServerIndexName());
                client.indices().refresh(request, RequestOptions.DEFAULT);
            }
            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);
            return amountDeleted;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Job save(Job job) {
        try {
            if (job.getId() == null) {
                job.setId(UUID.randomUUID());
            } else {
                job.increaseVersion();
            }

            IndexRequest request = new IndexRequest(jobIndexName())
                    .id(job.getId().toString())
                    .versionType(VersionType.EXTERNAL)
                    .version(job.getVersion())
                    .setRefreshPolicy(IMMEDIATE)
                    .source(elasticSearchDocumentMapper.toXContentBuilder(job));
            client.index(request, RequestOptions.DEFAULT);
            notifyJobStatsOnChangeListeners();
            return job;
        } catch (ElasticsearchStatusException e) {
            if (e.status().getStatus() == 409) {
                throw new ConcurrentJobModificationException(job);
            }
            throw e;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deletePermanently(UUID id) {
        try {
            DeleteResponse delete = client.delete(new DeleteRequest(jobIndexName(), id.toString()).setRefreshPolicy(IMMEDIATE), RequestOptions.DEFAULT);
            int amountDeleted = delete.getShardInfo().getSuccessful();
            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);
            return amountDeleted;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Job getJobById(UUID id) {
        try {
            GetRequest request = new GetRequest(jobIndexName(), id.toString());
            request.storedFields(Jobs.FIELD_JOB_AS_JSON);
            GetResponse response = client.get(request, RequestOptions.DEFAULT);
            if (response.isExists()) {
                return elasticSearchDocumentMapper.toJob(response);
            } else {
                throw new JobNotFoundException(id);
            }
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        try {
            Consumer<Job> jobConsumer;
            if (areNewJobs(jobs)) {
                if (notAllJobsAreNew(jobs)) {
                    throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
                }
                jobConsumer = job -> job.setId(UUID.randomUUID());
            } else {
                if (notAllJobsAreExisting(jobs)) {
                    throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
                }
                jobConsumer = AbstractJob::increaseVersion;

            }

            BulkRequest bulkRequest = new BulkRequest(jobIndexName()).setRefreshPolicy(IMMEDIATE);
            jobs.stream()
                    .peek(jobConsumer)
                    .map(job -> new IndexRequest().id(job.getId().toString())
                            .versionType(VersionType.EXTERNAL)
                            .version(job.getVersion())
                            .source(elasticSearchDocumentMapper.toXContentBuilder(job)))
                    .forEach(bulkRequest::add);

            BulkResponse bulk = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            List<Job> concurrentModifiedJobs = Stream.of(bulk.getItems()).filter(item -> item.isFailed() && item.status().getStatus() == 409).map(item -> jobs.get(item.getItemId())).collect(toList());
            if (!concurrentModifiedJobs.isEmpty()) {
                throw new ConcurrentJobModificationException(concurrentModifiedJobs);
            }
            notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
            return jobs;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        try {
            BoolQueryBuilder boolQueryBuilder = boolQuery()
                    .must(matchQuery(Jobs.FIELD_STATE, state))
                    .must(rangeQuery(Jobs.FIELD_UPDATED_AT).to(updatedBefore));

            SearchResponse searchResponse = searchJobs(boolQueryBuilder, pageRequest);
            return Stream.of(searchResponse.getHits().getHits())
                    .map(elasticSearchDocumentMapper::toJob)
                    .collect(toList());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        try {
            RangeQueryBuilder scheduledBeforeQuery = rangeQuery(Jobs.FIELD_SCHEDULED_AT).to(scheduledBefore);
            SearchResponse searchResponse = searchJobs(scheduledBeforeQuery, pageRequest);
            return Stream.of(searchResponse.getHits().getHits())
                    .map(elasticSearchDocumentMapper::toJob)
                    .collect(toList());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Long countJobs(StateName state) {
        try {
            return countJobs(boolQuery().must(matchQuery(Jobs.FIELD_STATE, state)));
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        try {
            BoolQueryBuilder stateQuery = boolQuery().must(matchQuery(Jobs.FIELD_STATE, state));
            SearchResponse searchResponse = searchJobs(stateQuery, pageRequest);
            return Stream.of(searchResponse.getHits().getHits())
                    .map(elasticSearchDocumentMapper::toJob)
                    .collect(toList());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        Long count = countJobs(state);
        if (count > 0) {
            List<Job> jobs = getJobs(state, pageRequest);
            return new Page<>(count, jobs, pageRequest);
        }
        return new Page<>(0, new ArrayList<>(), pageRequest);
    }

    @Override
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        try {
            BoolQueryBuilder boolQueryBuilder = boolQuery()
                    .must(matchQuery(Jobs.FIELD_STATE, state))
                    .must(rangeQuery(Jobs.FIELD_UPDATED_AT).to(updatedBefore));

            DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(jobIndexName());
            deleteByQueryRequest.setQuery(boolQueryBuilder);
            BulkByScrollResponse bulkByScrollResponse = client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
            int amountDeleted = (int) bulkByScrollResponse.getDeleted();
            if (amountDeleted > 0) {
                RefreshRequest request = new RefreshRequest(jobIndexName());
                client.indices().refresh(request, RequestOptions.DEFAULT);
            }
            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);
            return amountDeleted;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        try {
            BoolQueryBuilder stateQuery = boolQuery();
            for (StateName state : states) {
                stateQuery.should(matchQuery(Jobs.FIELD_STATE, state));
            }

            SearchRequest searchRequest = new SearchRequest(jobIndexName());
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(stateQuery);
            searchSourceBuilder.aggregation(AggregationBuilders.terms(Jobs.FIELD_JOB_SIGNATURE).field(Jobs.FIELD_JOB_SIGNATURE));
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            Terms terms = searchResponse.getAggregations().get(Jobs.FIELD_JOB_SIGNATURE);
            return terms.getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(toSet());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName... states) {
        try {
            BoolQueryBuilder stateQuery = boolQuery();
            for (StateName state : states) {
                stateQuery.should(matchQuery(Jobs.FIELD_STATE, state));
            }

            BoolQueryBuilder stateAndJobSignatureQuery = boolQuery()
                    .must(stateQuery)
                    .must(matchQuery(Jobs.FIELD_JOB_SIGNATURE, JobUtils.getJobSignature(jobDetails)));

            return countJobs(stateAndJobSignatureQuery) > 0;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        try {
            BoolQueryBuilder stateQuery = boolQuery();
            for (StateName state : states) {
                stateQuery.should(matchQuery(Jobs.FIELD_STATE, state));
            }

            BoolQueryBuilder stateAndJobSignatureQuery = boolQuery()
                    .must(stateQuery)
                    .must(matchQuery(Jobs.FIELD_RECURRING_JOB_ID, recurringJobId));

            return countJobs(stateAndJobSignatureQuery) > 0;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        try {
            IndexRequest request = new IndexRequest(recurringJobIndexName())
                    .id(recurringJob.getId())
                    .setRefreshPolicy(IMMEDIATE)
                    .source(elasticSearchDocumentMapper.toXContentBuilder(recurringJob));
            client.index(request, RequestOptions.DEFAULT);
            return recurringJob;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        try {
            SearchRequest searchRequest = new SearchRequest(recurringJobIndexName());
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            searchSourceBuilder.storedField(RecurringJobs.FIELD_JOB_AS_JSON);
            searchRequest.source(searchSourceBuilder);
            SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
            return Stream.of(search.getHits().getHits())
                    .map(elasticSearchDocumentMapper::toRecurringJob)
                    .collect(toList());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deleteRecurringJob(String id) {
        try {
            DeleteResponse delete = client.delete(new DeleteRequest(recurringJobIndexName(), id).setRefreshPolicy(IMMEDIATE), RequestOptions.DEFAULT);
            int amountDeleted = delete.getShardInfo().getSuccessful();
            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);
            return amountDeleted;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public JobStats getJobStats() {
        try {
            GetResponse getResponse = client.get(new GetRequest("jobrunr_job_stats", "job_stats"), RequestOptions.DEFAULT);

            SearchRequest searchRequest = new SearchRequest(jobIndexName());
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            searchSourceBuilder.aggregation(AggregationBuilders.terms(Jobs.FIELD_STATE).field(Jobs.FIELD_STATE));
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            Terms terms = searchResponse.getAggregations().get(Jobs.FIELD_STATE);
            List<? extends Terms.Bucket> buckets = terms.getBuckets();

            return new JobStats(
                    Instant.now(),
                    0L,
                    buckets.stream().filter(bucket -> StateName.AWAITING.name().equals(bucket.getKeyAsString())).map(MultiBucketsAggregation.Bucket::getDocCount).findFirst().orElse(0L) + (int) getResponse.getSource().getOrDefault(StateName.AWAITING.toString(), 0L),
                    buckets.stream().filter(bucket -> StateName.SCHEDULED.name().equals(bucket.getKeyAsString())).map(MultiBucketsAggregation.Bucket::getDocCount).findFirst().orElse(0L) + (int) getResponse.getSource().getOrDefault(StateName.SCHEDULED.toString(), 0L),
                    buckets.stream().filter(bucket -> StateName.ENQUEUED.name().equals(bucket.getKeyAsString())).map(MultiBucketsAggregation.Bucket::getDocCount).findFirst().orElse(0L) + (int) getResponse.getSource().getOrDefault(StateName.ENQUEUED.toString(), 0L),
                    buckets.stream().filter(bucket -> StateName.PROCESSING.name().equals(bucket.getKeyAsString())).map(MultiBucketsAggregation.Bucket::getDocCount).findFirst().orElse(0L) + (int) getResponse.getSource().getOrDefault(StateName.PROCESSING.toString(), 0L),
                    buckets.stream().filter(bucket -> StateName.FAILED.name().equals(bucket.getKeyAsString())).map(MultiBucketsAggregation.Bucket::getDocCount).findFirst().orElse(0L) + (int) getResponse.getSource().getOrDefault(StateName.FAILED.toString(), 0L),
                    buckets.stream().filter(bucket -> StateName.SUCCEEDED.name().equals(bucket.getKeyAsString())).map(MultiBucketsAggregation.Bucket::getDocCount).findFirst().orElse(0L) + (int) getResponse.getSource().getOrDefault(StateName.SUCCEEDED.toString(), 0L),
                    buckets.stream().filter(bucket -> StateName.DELETED.name().equals(bucket.getKeyAsString())).map(MultiBucketsAggregation.Bucket::getDocCount).findFirst().orElse(0L) + (int) getResponse.getSource().getOrDefault(StateName.DELETED.toString(), 0L),
                    (int) client.count(new CountRequest(recurringJobIndexName()), RequestOptions.DEFAULT).getCount(),
                    (int) client.count(new CountRequest(backgroundJobServerIndexName()), RequestOptions.DEFAULT).getCount()
            );
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void publishJobStatCounter(StateName state, int amount) {
        try {
            UpdateRequest updateRequest = new UpdateRequest("jobrunr_job_stats", "job_stats");
            updateRequest.scriptedUpsert(true);
            Map<String, Object> parameters = singletonMap("amount", amount);
            Script inline = new Script(ScriptType.INLINE, "painless",
                    "ctx._source." + state + " += params.amount", parameters);
            updateRequest.script(inline);
            client.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    long countJobs(QueryBuilder queryBuilder) throws IOException {
        CountRequest countRequest = new CountRequest(jobIndexName());
        countRequest.query(queryBuilder);
        CountResponse countResponse = client.count(countRequest, RequestOptions.DEFAULT);
        return countResponse.getCount();
    }

    SearchResponse searchJobs(QueryBuilder queryBuilder, PageRequest pageRequest) throws IOException {
        SearchRequest searchRequest = new SearchRequest(jobIndexName());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.from((int) pageRequest.getOffset());
        searchSourceBuilder.size(pageRequest.getLimit());
        searchSourceBuilder.storedField(Jobs.FIELD_JOB_AS_JSON);
        if (pageRequest.getOrder().equals("updatedAt:ASC")) {
            searchSourceBuilder.sort(Jobs.FIELD_UPDATED_AT, SortOrder.ASC);
        } else if (pageRequest.getOrder().equals("updatedAt:DESC")) {
            searchSourceBuilder.sort(Jobs.FIELD_UPDATED_AT, SortOrder.DESC);
        } else {
            throw new IllegalArgumentException("Unknown sort: " + pageRequest.getOrder());
        }
        searchRequest.source(searchSourceBuilder);
        return client.search(searchRequest, RequestOptions.DEFAULT);
    }

    void createIndicesIfNecessary() {
        createIndicesIfNecessary(0);
    }

    void createIndicesIfNecessary(int retry) {
        try {
            Thread.sleep(retry * 500);
            if (client.indices().exists(new GetIndexRequest(jobIndexName()), RequestOptions.DEFAULT)) return;

            try {
                client.indices().create(jobIndex(), RequestOptions.DEFAULT);
                client.indices().create(recurringJobIndex(), RequestOptions.DEFAULT);
                client.indices().create(backgroundJobServersIndex(), RequestOptions.DEFAULT);
                client.index(jobStatsIndex(), RequestOptions.DEFAULT);
            } catch (ElasticsearchStatusException e) {
                if (retry >= 5) {
                    throw new StorageException("Retried 5 times to setup ElasticSearch Indices", e);
                } else if (e.status().getStatus() == 400) {
                    createIndicesIfNecessary(retry + 1);
                } else {
                    throw e;
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new StorageException(e);
        }
    }
}
