package org.jobrunr.storage.nosql.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
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
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.jobrunr.jobs.*;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.*;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;
import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.utils.annotations.Beta;
import org.jobrunr.utils.resilience.RateLimiter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.sum;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.storage.JobRunrMetadata.toId;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.*;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.CREATE;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.*;
import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.storage.StorageProviderUtils.Metadata.FIELD_VALUE;
import static org.jobrunr.storage.StorageProviderUtils.Metadata.STATS_ID;
import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs.FIELD_CREATED_AT;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.JOBRUNR_PREFIX;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

@Beta(note = "The ElasticSearchStorageProvider is still in Beta. My first impression is that other StorageProviders are faster than ElasticSearch.")
public class ElasticSearchStorageProvider extends AbstractStorageProvider implements NoSqlStorageProvider {

    public static final String DEFAULT_JOB_INDEX_NAME = JOBRUNR_PREFIX + Jobs.NAME;
    public static final String DEFAULT_RECURRING_JOB_INDEX_NAME = JOBRUNR_PREFIX + RecurringJobs.NAME;
    public static final String DEFAULT_BACKGROUND_JOB_SERVER_INDEX_NAME = JOBRUNR_PREFIX + BackgroundJobServers.NAME;
    public static final String DEFAULT_METADATA_INDEX_NAME = JOBRUNR_PREFIX + Metadata.NAME;
    public static final int MAX_SIZE = 10_000;

    private final RestHighLevelClient client;
    private final String jobIndexName;
    private final String recurringJobIndexName;
    private final String backgroundJobServerIndexName;
    private final String metadataIndexName;
    private final String indexPrefix;

    private ElasticSearchDocumentMapper documentMapper;


    public ElasticSearchStorageProvider(String hostName, int port) {
        this(new HttpHost(hostName, port, "http"));
    }

    public ElasticSearchStorageProvider(HttpHost httpHost) {
        this(new RestHighLevelClient(RestClient.builder(httpHost)));
    }

    public ElasticSearchStorageProvider(RestHighLevelClient client) {
        this(client, (String) null);
    }

    public ElasticSearchStorageProvider(RestHighLevelClient client, String indexPrefix) {
        this(client, indexPrefix, CREATE, rateLimit().at1Request().per(SECOND));
    }

    public ElasticSearchStorageProvider(RestHighLevelClient client, String indexPrefix, DatabaseOptions databaseOptions) {
        this(client, indexPrefix, databaseOptions, rateLimit().at1Request().per(SECOND));
    }

    public ElasticSearchStorageProvider(RestHighLevelClient client, DatabaseOptions databaseOptions) {
        this(client, null, databaseOptions, rateLimit().at1Request().per(SECOND));
    }

    public ElasticSearchStorageProvider(RestHighLevelClient client, DatabaseOptions databaseOptions, RateLimiter rateLimiter) {
        this(client, null, databaseOptions, rateLimiter);
    }

    public ElasticSearchStorageProvider(RestHighLevelClient client, String indexPrefix, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        this.client = client;
        this.indexPrefix = indexPrefix;

        setUpStorageProvider(databaseOptions);

        this.jobIndexName = elementPrefixer(indexPrefix, DEFAULT_JOB_INDEX_NAME);
        this.recurringJobIndexName = elementPrefixer(indexPrefix, DEFAULT_RECURRING_JOB_INDEX_NAME);
        this.backgroundJobServerIndexName = elementPrefixer(indexPrefix, DEFAULT_BACKGROUND_JOB_SERVER_INDEX_NAME);
        this.metadataIndexName = elementPrefixer(indexPrefix, DEFAULT_METADATA_INDEX_NAME);
    }

    @Override
    public void setJobMapper(final JobMapper jobMapper) {
        this.documentMapper = new ElasticSearchDocumentMapper(jobMapper);
    }

    @Override
    public void setUpStorageProvider(final DatabaseOptions options) {
        final ElasticSearchDBCreator creator = new ElasticSearchDBCreator(this, client, indexPrefix);
        if (DatabaseOptions.CREATE == options) {
            creator.runMigrations();
        } else {
            creator.validateIndices();
        }
    }

    @Override
    public void announceBackgroundJobServer(final BackgroundJobServerStatus status) {
        try {
            final IndexRequest request = new IndexRequest()
                    .index(backgroundJobServerIndexName)
                    .id(status.getId().toString())
                    .setRefreshPolicy(IMMEDIATE)
                    .source(documentMapper.toXContentBuilderForInsert(status));

            client.index(request, DEFAULT);
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean signalBackgroundJobServerAlive(final BackgroundJobServerStatus status) {
        try {
            final UpdateRequest updateRequest = new UpdateRequest()
                    .index(backgroundJobServerIndexName)
                    .id(status.getId().toString())
                    .fetchSource(true)
                    .doc(documentMapper.toXContentBuilderForUpdate(status));

            final UpdateResponse updateResponse = client.update(updateRequest, DEFAULT);

            return cast(updateResponse.getGetResult().getSource().getOrDefault(FIELD_IS_RUNNING, false));
        } catch (final ElasticsearchStatusException e) {
            if (e.status().getStatus() == 404) {
                throw new ServerTimedOutException(status, new StorageException(e));
            }
            throw e;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void signalBackgroundJobServerStopped(final BackgroundJobServerStatus serverStatus) {
        try {
            final String id = serverStatus.getId().toString();
            final DeleteRequest request = new DeleteRequest(backgroundJobServerIndexName, id);
            client.delete(request.setRefreshPolicy(IMMEDIATE), DEFAULT);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        try {
            final SearchSourceBuilder source = new SearchSourceBuilder()
                    .query(matchAllQuery())
                    .fetchSource(true)
                    .sort(FIELD_FIRST_HEARTBEAT, ASC)
                    .size(MAX_SIZE);

            SearchResponse search = client.search(new SearchRequest()
                    .indices(backgroundJobServerIndexName)
                    .source(source), DEFAULT);

            return Stream.of(search.getHits().getHits())
                    .map(documentMapper::toBackgroundJobServerStatus)
                    .collect(toList());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public UUID getLongestRunningBackgroundJobServerId() {
        try {
            final SearchSourceBuilder source = new SearchSourceBuilder()
                    .query(matchAllQuery())
                    .fetchSource(false)
                    .sort(FIELD_FIRST_HEARTBEAT, ASC)
                    .size(1);

            final SearchResponse search = client.search(new SearchRequest()
                    .indices(backgroundJobServerIndexName)
                    .source(source), DEFAULT);

            return UUID.fromString(search.getHits().getHits()[0].getId());
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int removeTimedOutBackgroundJobServers(final Instant heartbeatOlderThan) {
        final long deleted = deleteByQuery(
                backgroundJobServerIndexName,
                rangeQuery(FIELD_LAST_HEARTBEAT).to(heartbeatOlderThan)
        );

        notifyJobStatsOnChangeListenersIf(deleted > 0);

        return (int) deleted;
    }

    @Override
    public void saveMetadata(final JobRunrMetadata metadata) {
        try {
            final IndexRequest request = new IndexRequest()
                    .index(metadataIndexName)
                    .id(metadata.getId())
                    .setRefreshPolicy(IMMEDIATE)
                    .source(documentMapper.toXContentBuilder(metadata));

            client.index(request, DEFAULT);

            notifyMetadataChangeListeners();
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<JobRunrMetadata> getMetadata(final String name) {
        try {
            final SearchSourceBuilder source = new SearchSourceBuilder()
                    .query(matchQuery(Metadata.FIELD_NAME, name))
                    .size(MAX_SIZE);

            final SearchResponse response = client.search(new SearchRequest()
                    .indices(metadataIndexName)
                    .source(source), DEFAULT);

            return Stream.of(response.getHits().getHits())
                    .map(documentMapper::toMetadata)
                    .collect(toList());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public JobRunrMetadata getMetadata(final String name, final String owner) {
        try {
            final GetRequest request = new GetRequest(metadataIndexName, toId(name, owner));
            final GetResponse response = client.get(request, DEFAULT);
            return documentMapper.toMetadata(response);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void deleteMetadata(final String name) {
        final long deleted = deleteByQuery(metadataIndexName, matchQuery(Metadata.FIELD_NAME, name));

        if (deleted > 0) {
            notifyMetadataChangeListeners();
        }
    }

    private long deleteByQuery(final String index, final QueryBuilder query) {
        final DeleteByQueryRequest request = new DeleteByQueryRequest(index)
                .setQuery(query)
                .setRefresh(true);

        try {
            final BulkByScrollResponse response = client.deleteByQuery(request, DEFAULT);
            return response.getDeleted();
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Job save(final Job job) {
        try (JobVersioner jobVersioner = new JobVersioner(job)) {
            IndexRequest request = new IndexRequest()
                    .index(jobIndexName)
                    .id(job.getId().toString())
                    .versionType(VersionType.EXTERNAL)
                    .version(job.getVersion())
                    .setRefreshPolicy(IMMEDIATE)
                    .source(documentMapper.toXContentBuilder(job));

            client.index(request, DEFAULT);
            jobVersioner.commitVersion();
            notifyJobStatsOnChangeListeners();
            return job;
        } catch (final ElasticsearchException e) {
            if (e.status().getStatus() == 409) {
                throw new ConcurrentJobModificationException(job);
            }
            throw new StorageException(e);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deletePermanently(final UUID id) {
        try {
            final DeleteRequest request = new DeleteRequest()
                    .index(jobIndexName)
                    .id(id.toString())
                    .setRefreshPolicy(IMMEDIATE);

            final DeleteResponse response = client.delete(request, DEFAULT);
            final int amountDeleted = response.getShardInfo().getSuccessful();

            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);
            return amountDeleted;
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Job getJobById(final UUID id) {
        try {
            final GetRequest request = new GetRequest()
                    .index(jobIndexName)
                    .id(id.toString())
                    .storedFields(Jobs.FIELD_JOB_AS_JSON);

            final GetResponse response = client.get(request, DEFAULT);
            if (response.isExists()) {
                return documentMapper.toJob(response);
            }

            throw new JobNotFoundException(id);
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> save(final List<Job> jobs) {
        try (final JobListVersioner versioner = new JobListVersioner(jobs)) {
            versioner.validateJobs();

            final BulkRequest bulkRequest = new BulkRequest(jobIndexName)
                    .setRefreshPolicy(IMMEDIATE);
            jobs.stream()
                    .map(job ->
                            new IndexRequest()
                                    .id(job.getId().toString())
                                    .versionType(VersionType.EXTERNAL)
                                    .version(job.getVersion())
                                    .source(documentMapper.toXContentBuilder(job))
                    ).forEach(bulkRequest::add);

            final BulkResponse response = client.bulk(bulkRequest, DEFAULT);
            final List<Job> concurrentModifiedJobs = Stream.of(response.getItems())
                    .filter(item -> item.isFailed() && item.status().getStatus() == 409)
                    .map(item -> jobs.get(item.getItemId()))
                    .collect(toList());
            if (!concurrentModifiedJobs.isEmpty()) {
                versioner.rollbackVersions(concurrentModifiedJobs);
                throw new ConcurrentJobModificationException(concurrentModifiedJobs);
            }

            versioner.commitVersions();

            notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
            return jobs;
        } catch (final ElasticsearchException | IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getJobs(final StateName state, final Instant updatedBefore, final PageRequest pageRequest) {
        try {
            final BoolQueryBuilder query = boolQuery()
                    .must(matchQuery(FIELD_STATE, state))
                    .must(rangeQuery(Jobs.FIELD_UPDATED_AT).to(updatedBefore));

            final SearchResponse response = searchJobs(query, pageRequest);
            return Stream.of(response.getHits().getHits())
                    .map(documentMapper::toJob)
                    .collect(toList());
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getScheduledJobs(final Instant scheduledBefore, final PageRequest pageRequest) {
        try {
            final QueryBuilder query = rangeQuery(Jobs.FIELD_SCHEDULED_AT).to(scheduledBefore);

            final SearchResponse searchResponse = searchJobs(query, pageRequest);

            return Stream.of(searchResponse.getHits().getHits())
                    .map(documentMapper::toJob)
                    .collect(toList());
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getJobs(final StateName state, final PageRequest pageRequest) {
        try {
            final QueryBuilder query = boolQuery().must(matchQuery(FIELD_STATE, state));
            final SearchResponse response = searchJobs(query, pageRequest);

            return Stream.of(response.getHits().getHits())
                    .map(documentMapper::toJob)
                    .collect(toList());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        try {
            long count = countJobs(boolQuery().must(matchQuery(FIELD_STATE, state)));
            if (count > 0) {
                List<Job> jobs = getJobs(state, pageRequest);
                return new Page<>(count, jobs, pageRequest);
            }
            return new Page<>(0, new ArrayList<>(), pageRequest);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        try {
            final QueryBuilder query = boolQuery()
                    .must(matchQuery(FIELD_STATE, state))
                    .must(rangeQuery(Jobs.FIELD_UPDATED_AT).to(updatedBefore));

            final DeleteByQueryRequest request = new DeleteByQueryRequest(jobIndexName)
                    .setQuery(query)
                    .setRefresh(true);

            final BulkByScrollResponse response = client.deleteByQuery(request, DEFAULT);

            final int amountDeleted = (int) response.getDeleted();

            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);
            return amountDeleted;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Set<String> getDistinctJobSignatures(final StateName... states) {
        try {
            final SearchSourceBuilder source = new SearchSourceBuilder()
                    .query(shouldMatch(states))
                    .aggregation(terms(FIELD_JOB_SIGNATURE).field(FIELD_JOB_SIGNATURE));

            final SearchResponse response = client.search(new SearchRequest(jobIndexName)
                    .source(source), DEFAULT);
            final Terms terms = response.getAggregations().get(FIELD_JOB_SIGNATURE);

            return terms
                    .getBuckets()
                    .stream()
                    .map(MultiBucketsAggregation.Bucket::getKeyAsString)
                    .collect(toSet());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean exists(final JobDetails jobDetails, final StateName... states) {
        try {
            final QueryBuilder query = boolQuery()
                    .must(shouldMatch(states))
                    .must(matchQuery(FIELD_JOB_SIGNATURE, getJobSignature(jobDetails)));

            return countJobs(query) > 0;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean recurringJobExists(final String recurringJobId, final StateName... states) {
        try {
            final QueryBuilder query = boolQuery()
                    .must(shouldMatch(states))
                    .must(matchQuery(FIELD_RECURRING_JOB_ID, recurringJobId));

            return countJobs(query) > 0;
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public RecurringJob saveRecurringJob(final RecurringJob job) {
        try {
            final IndexRequest request = new IndexRequest()
                    .index(recurringJobIndexName)
                    .id(job.getId())
                    .setRefreshPolicy(IMMEDIATE)
                    .source(documentMapper.toXContentBuilder(job));

            client.index(request, DEFAULT);

            return job;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public RecurringJobsResult getRecurringJobs() {
        try {
            final SearchSourceBuilder source = new SearchSourceBuilder()
                    .query(matchAllQuery())
                    .storedField(RecurringJobs.FIELD_JOB_AS_JSON)
                    .size(MAX_SIZE);

            final SearchResponse response = client.search(new SearchRequest()
                    .indices(recurringJobIndexName)
                    .source(source), DEFAULT);

            final List<RecurringJob> jobs = Stream.of(response.getHits().getHits())
                    .map(documentMapper::toRecurringJob)
                    .collect(toList());
            return new RecurringJobsResult(jobs);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean recurringJobsUpdated(final Long recurringJobsUpdatedHash) {
        try {
            final SearchSourceBuilder source = new SearchSourceBuilder()
                    .query(matchAllQuery())
                    .aggregation(sum(FIELD_CREATED_AT).field(FIELD_CREATED_AT))
                    .size(0);

            final SearchRequest search = new SearchRequest()
                    .indices(recurringJobIndexName)
                    .source(source);

            final SearchResponse response = client.search(search, DEFAULT);
            final ParsedSum parsedSum = response.getAggregations().get(FIELD_CREATED_AT);

            return !recurringJobsUpdatedHash.equals(Double.valueOf(parsedSum.getValue()).longValue());
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public long countRecurringJobs() {
        try {
            return getCount(recurringJobIndexName);
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deleteRecurringJob(final String id) {
        try {
            final DeleteRequest request = new DeleteRequest()
                    .index(recurringJobIndexName)
                    .id(id)
                    .setRefreshPolicy(IMMEDIATE);

            final DeleteResponse response = client.delete(request, DEFAULT);
            final int amountDeleted = response.getShardInfo().getSuccessful();

            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);

            return amountDeleted;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public JobStats getJobStats() {
        try {
            Instant instant = Instant.now();
            final SearchSourceBuilder source = new SearchSourceBuilder()
                    .query(matchAllQuery())
                    .aggregation(terms(FIELD_STATE).field(FIELD_STATE))
                    .size(0);

            final SearchRequest request = new SearchRequest(jobIndexName)
                    .source(source);

            final SearchResponse response = client.search(request, DEFAULT);

            final Terms terms = response.getAggregations().get(FIELD_STATE);
            List<? extends Terms.Bucket> buckets = terms.getBuckets();

            final long allTimeSucceededJobs = getAllTimeSucceededJobs();
            final int recurringJobs = getCount(recurringJobIndexName);
            final int backgroundJobServers = getCount(backgroundJobServerIndexName);

            return new JobStats(
                    instant,
                    0L,
                    count(buckets, SCHEDULED),
                    count(buckets, ENQUEUED),
                    count(buckets, PROCESSING),
                    count(buckets, FAILED),
                    count(buckets, SUCCEEDED),
                    allTimeSucceededJobs,
                    count(buckets, DELETED),
                    recurringJobs,
                    backgroundJobServers
            );
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    private long getAllTimeSucceededJobs() throws IOException {
        final GetRequest request = new GetRequest()
                .index(metadataIndexName)
                .id(STATS_ID);

        final GetResponse response = client.get(request, DEFAULT);
        final Object value = response.getSource().getOrDefault(FIELD_VALUE, 0L);

        return ((Number) value).longValue();
    }

    private int getCount(final String indexName) throws IOException {
        final CountRequest request = new CountRequest()
                .indices(indexName);
        return (int) client.count(request, DEFAULT).getCount();
    }

    private long count(final List<? extends Terms.Bucket> buckets, final StateName state) {
        return buckets
                .stream()
                .filter(bucket -> Objects.equals(state.name(), bucket.getKeyAsString()))
                .map(MultiBucketsAggregation.Bucket::getDocCount)
                .findFirst()
                .orElse(0L);
    }

    @Override
    public void publishTotalAmountOfSucceededJobs(final int amount) {
        try {
            final UpdateRequest update = new UpdateRequest(metadataIndexName, STATS_ID)
                    .scriptedUpsert(true);

            final Map<String, Object> parameters = singletonMap("value", amount);
            final Script inline = new Script(ScriptType.INLINE, "painless", "ctx._source." + FIELD_VALUE + " += params.value", parameters);

            update.script(inline);
            client.update(update, DEFAULT);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    long countJobs(final QueryBuilder query) throws IOException {
        final CountRequest request = new CountRequest()
                .indices(jobIndexName)
                .query(query);

        final CountResponse response = client.count(request, DEFAULT);

        return response.getCount();
    }

    SearchResponse searchJobs(final QueryBuilder query, final PageRequest page) throws IOException {
        final SearchSourceBuilder source = new SearchSourceBuilder()
                .query(query)
                .from((int) page.getOffset())
                .size(page.getLimit())
                .storedField(Jobs.FIELD_JOB_AS_JSON);

        sortJobs(page, source);

        final SearchRequest request = new SearchRequest(jobIndexName)
                .source(source);

        return client.search(request, DEFAULT);
    }

    private static QueryBuilder shouldMatch(final StateName... states) {
        final BoolQueryBuilder query = boolQuery();
        for (final StateName state : states) {
            query.should(matchQuery(FIELD_STATE, state));
        }
        return query;
    }

    private static void sortJobs(final PageRequest page, final SearchSourceBuilder source) {
        final String order = page.getOrder();
        if (Objects.equals(order, "updatedAt:ASC")) {
            source.sort(Jobs.FIELD_UPDATED_AT, ASC);
        } else if (Objects.equals(order, "updatedAt:DESC")) {
            source.sort(Jobs.FIELD_UPDATED_AT, SortOrder.DESC);
        } else {
            throw new IllegalArgumentException("Unknown sort: " + order);
        }
    }
}
