package org.jobrunr.storage.nosql.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryVariant;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobListVersioner;
import org.jobrunr.jobs.JobVersioner;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.AbstractStorageProvider;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.storage.navigation.OrderTerm;
import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.utils.annotations.Beta;
import org.jobrunr.utils.resilience.RateLimiter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static co.elastic.clients.elasticsearch._types.Refresh.True;
import static co.elastic.clients.elasticsearch._types.SortOrder.Asc;
import static co.elastic.clients.elasticsearch._types.SortOrder.Desc;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.bool;
import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.jobrunr.jobs.Job.ALLOWED_SORT_COLUMNS;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.JobRunrMetadata.toId;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_FIRST_HEARTBEAT;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_IS_RUNNING;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_NAME;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.CREATE;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.SKIP_CREATE;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_JOB_SIGNATURE;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_RECURRING_JOB_ID;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_STATE;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_UPDATED_AT;
import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.storage.StorageProviderUtils.Metadata.FIELD_VALUE;
import static org.jobrunr.storage.StorageProviderUtils.Metadata.STATS_ID;
import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs.FIELD_CREATED_AT;
import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs.FIELD_JOB_AS_JSON;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.JOBRUNR_PREFIX;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

@Deprecated
@Beta(note = "The ElasticSearchStorageProvider is still in Beta. My first impression is that other StorageProviders are faster than ElasticSearch.")
public class ElasticSearchStorageProvider extends AbstractStorageProvider implements NoSqlStorageProvider {

    @SuppressWarnings("unchecked")
    private static final Class<Map> MAP_CLASS = Map.class;

    public static final String DEFAULT_JOB_INDEX_NAME = JOBRUNR_PREFIX + Jobs.NAME;
    public static final String DEFAULT_RECURRING_JOB_INDEX_NAME = JOBRUNR_PREFIX + RecurringJobs.NAME;
    public static final String DEFAULT_BACKGROUND_JOB_SERVER_INDEX_NAME = JOBRUNR_PREFIX + BackgroundJobServers.NAME;
    public static final String DEFAULT_METADATA_INDEX_NAME = JOBRUNR_PREFIX + Metadata.NAME;
    public static final int MAX_SIZE = 10_000;

    private final ElasticsearchClient client;
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
        this(newClient(httpHost));
    }

    public ElasticSearchStorageProvider(ElasticsearchClient client) {
        this(client, (String) null);
    }

    public ElasticSearchStorageProvider(ElasticsearchClient client, String indexPrefix) {
        this(client, indexPrefix, CREATE, rateLimit().at1Request().per(SECOND));
    }

    public ElasticSearchStorageProvider(ElasticsearchClient client, String indexPrefix, DatabaseOptions databaseOptions) {
        this(client, indexPrefix, databaseOptions, rateLimit().at1Request().per(SECOND));
    }

    public ElasticSearchStorageProvider(ElasticsearchClient client, DatabaseOptions databaseOptions) {
        this(client, null, databaseOptions, rateLimit().at1Request().per(SECOND));
    }

    public ElasticSearchStorageProvider(ElasticsearchClient client, DatabaseOptions databaseOptions, RateLimiter rateLimiter) {
        this(client, null, databaseOptions, rateLimiter);
    }

    public ElasticSearchStorageProvider(ElasticsearchClient client, String indexPrefix, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
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
    public void setUpStorageProvider(final DatabaseOptions databaseOptions) {
        final ElasticSearchDBCreator creator = new ElasticSearchDBCreator(this, client, indexPrefix);
        if (databaseOptions == CREATE) {
            creator.runMigrations();
        } else if (databaseOptions == SKIP_CREATE) {
            creator.validateIndices();
        }
    }

    @Override
    public void announceBackgroundJobServer(final BackgroundJobServerStatus status) {
        try {
            client.index(r ->
                    r.index(backgroundJobServerIndexName)
                            .id(status.getId().toString())
                            .refresh(True)
                            .document(documentMapper.toMap(status))
            );
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean signalBackgroundJobServerAlive(final BackgroundJobServerStatus status) {
        try {
            final Map<Object, Object> value = documentMapper.toMapForUpdate(status);
            final UpdateResponse<? extends Map> response = client.update(r ->
                            r
                                    .index(backgroundJobServerIndexName)
                                    .id(status.getId().toString())
                                    .refresh(True)
                                    .source(s -> s.fetch(true))
                                    .doc(value),
                    value.getClass()
            );

            final Map<Object, Object> source = cast(response.get().source());
            return cast(source.getOrDefault(FIELD_IS_RUNNING, false));
        } catch (final ElasticsearchException e) {
            if (e.status() == SC_NOT_FOUND) {
                throw new ServerTimedOutException(status, new StorageException(e));
            }
            throw e;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void signalBackgroundJobServerStopped(final BackgroundJobServerStatus status) {
        try {
            final String id = status.getId().toString();
            client.delete(d -> d
                    .index(backgroundJobServerIndexName)
                    .id(id)
                    .refresh(True)
            );
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        try {
            final SearchResponse<Map> search = client.search(s ->
                            s
                                    .index(backgroundJobServerIndexName)
                                    .query(q -> q.matchAll(m -> m))
                                    .sort(sort -> sort.field(f -> f.field(FIELD_FIRST_HEARTBEAT).order(Asc)))
                                    .source(src -> src.fetch(true))
                                    .size(MAX_SIZE),
                    MAP_CLASS
            );

            return search
                    .hits()
                    .hits()
                    .stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(documentMapper::toBackgroundJobServerStatus)
                    .collect(toList());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public UUID getLongestRunningBackgroundJobServerId() {
        try {
            final SearchResponse<?> search = client.search(s ->
                            s
                                    .index(backgroundJobServerIndexName)
                                    .query(q -> q.matchAll(m -> m))
                                    .source(src -> src.fetch(false))
                                    .sort(sort -> sort.field(f -> f.field(FIELD_FIRST_HEARTBEAT).order(Asc)))
                                    .size(1),
                    MAP_CLASS
            );

            return fromString(search.hits().hits().get(0).id());
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int removeTimedOutBackgroundJobServers(final Instant heartbeatOlderThan) {
        final RangeQuery q = RangeQuery
                .of(
                        r -> r.date(d -> d
                                .field(FIELD_LAST_HEARTBEAT)
                                .to(Long.toString(heartbeatOlderThan.toEpochMilli()))
                        )
                );

        final long deleted = deleteByQuery(backgroundJobServerIndexName, q);

        notifyJobStatsOnChangeListenersIf(deleted > 0);

        return (int) deleted;
    }

    @Override
    public void saveMetadata(final JobRunrMetadata metadata) {
        try {
            client.index(
                    i -> i
                            .index(metadataIndexName)
                            .id(metadata.getId())
                            .refresh(True)
                            .document(documentMapper.toMap(metadata))
            );

            notifyMetadataChangeListeners();
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<JobRunrMetadata> getMetadata(final String name) {
        try {
            final SearchResponse<Map> response = client.search(
                    s -> s
                            .index(metadataIndexName)
                            .query(q -> q.match(m -> m.field(FIELD_NAME).query(name)))
                            .source(src -> src.fetch(true))
                            .size(MAX_SIZE),
                    MAP_CLASS
            );

            return response
                    .hits()
                    .hits()
                    .stream()
                    .map(Hit::source)
                    .map(documentMapper::toMetadata)
                    .collect(toList());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public JobRunrMetadata getMetadata(final String name, final String owner) {
        try {
            final GetResponse<Map> response = client.get(
                    g -> g.index(metadataIndexName).id(toId(name, owner)),
                    MAP_CLASS
            );

            final Map<Object, Object> source = ofNullable(response.source())
                    .orElse(EMPTY_MAP);

            return documentMapper.toMetadata(source);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void deleteMetadata(final String name) {
        final MatchQuery q = MatchQuery.of(m -> m.field(FIELD_NAME).query(name));
        final long deleted = deleteByQuery(metadataIndexName, q);

        if (deleted > 0) {
            notifyMetadataChangeListeners();
        }
    }

    private long deleteByQuery(final String index, final QueryVariant query) {
        try {
            final DeleteByQueryResponse response = client.deleteByQuery(
                    d -> d
                            .index(index)
                            .query(query._toQuery())
                            .refresh(true)
            );
            return response.deleted();
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Job save(final Job job) {
        try (JobVersioner jobVersioner = new JobVersioner(job)) {
            client.index(
                    i -> i
                            .index(jobIndexName)
                            .id(job.getId().toString())
                            .versionType(VersionType.External)
                            .version((long) job.getVersion())
                            .document(documentMapper.toMap(job))
                            .refresh(True)
            );

            jobVersioner.commitVersion();
            notifyJobStatsOnChangeListeners();

            return job;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine() != null && e.getResponse().getStatusLine().getStatusCode() == SC_CONFLICT) {
                throw new ConcurrentJobModificationException(job);
            }
            throw new StorageException(e);
        } catch (final ElasticsearchException | IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deletePermanently(final UUID id) {
        try {
            final DeleteResponse response = client.delete(
                    d -> d
                            .index(jobIndexName)
                            .id(id.toString())
                            .refresh(True)
            );
            final int amountDeleted = response
                    .shards()
                    .successful()
                    .intValue();

            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);
            return amountDeleted;
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Job getJobById(final UUID id) {
        try {
            final GetResponse<Map> response = client.get(
                    r -> r
                            .index(jobIndexName)
                            .id(id.toString())
                            .storedFields(Jobs.FIELD_JOB_AS_JSON),
                    MAP_CLASS
            );

            if (response.found()) {
                return documentMapper.toJob(response);
            }

            throw new JobNotFoundException(id);
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public long countJobs(StateName state) {
        try {
            final QueryVariant query = bool()
                    .must(must -> must.match(match -> match.field(FIELD_STATE).query(state.toString())))
                    .build();
            return countJobs(query);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getJobList(StateName state, Instant updatedBefore, AmountRequest amountRequest) {
        final QueryVariant query = withStateAndUpdatedBefore(state, updatedBefore);
        return findJobs(query, amountRequest);
    }

    @Override
    public List<Job> getJobList(StateName state, AmountRequest amountRequest) {
        final QueryVariant query = bool()
                .must(must -> must.match(match -> match.field(FIELD_STATE).query(state.toString())))
                .build();
        return findJobs(query, amountRequest);
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, AmountRequest amountRequest) {
        final QueryVariant query = QueryBuilders.range()
                .date(d -> d
                        .field(FIELD_SCHEDULED_AT)
                        .to(Long.toString(scheduledBefore.toEpochMilli()))
                ).build();
        return findJobs(query, amountRequest);
    }

    @Override
    public List<Job> save(final List<Job> jobs) {
        if (jobs.isEmpty()) return jobs;

        try (final JobListVersioner versioner = new JobListVersioner(jobs)) {
            versioner.validateJobs();

            final BulkRequest.Builder bulk = new BulkRequest.Builder()
                    .index(jobIndexName)
                    .refresh(True);

            final List<BulkOperation> operations = new ArrayList<>();
            for (final Job job : jobs) {
                final IndexOperation.Builder<Map<Object, Object>> builder = new IndexOperation
                        .Builder<Map<Object, Object>>()
                        .id(job.getId().toString())
                        .versionType(VersionType.External)
                        .version((long) job.getVersion())
                        .document(documentMapper.toMap(job));

                operations.add(builder.build()._toBulkOperation());
            }

            bulk.operations(operations);

            final BulkResponse response = client.bulk(bulk.build());

            if (response.errors()) {
                final List<Job> concurrentModifiedJobs = new ArrayList<>();
                final List<BulkResponseItem> items = response.items();
                for (int i = 0; i < response.items().size(); i++) {
                    if (items.get(i).status() == 409) {
                        concurrentModifiedJobs.add(jobs.get(i));
                    }
                }
                if (!concurrentModifiedJobs.isEmpty()) {
                    versioner.rollbackVersions(concurrentModifiedJobs);
                    throw new ConcurrentJobModificationException(concurrentModifiedJobs);
                }
                throw new StorageException("Could not save all jobs");
            }

            versioner.commitVersions();
            notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
            return jobs;
        } catch (final ElasticsearchException | IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        try {
            final QueryVariant query = withStateAndUpdatedBefore(state, updatedBefore);

            final DeleteByQueryResponse response = client.deleteByQuery(
                    d -> d.index(jobIndexName)
                            .query(query._toQuery())
                            .refresh(true)
            );

            final int amountDeleted = response.deleted().intValue();

            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);

            return amountDeleted;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    private static QueryVariant withStateAndUpdatedBefore(final StateName state, final Instant updatedBefore) {
        return bool()
                .must(must -> must.match(m -> m.field(FIELD_STATE).query(String.valueOf(state))))
                .must(must -> must.range(m -> m.date(d -> d.field(FIELD_UPDATED_AT).to(Long.toString(updatedBefore.toEpochMilli())))))
                .build();
    }

    @Override
    public Set<String> getDistinctJobSignatures(final StateName... states) {
        try {
            final SearchResponse<Map> response = client.search(
                    s -> s
                            .index(jobIndexName)
                            .query(shouldMatch(states))
                            .aggregations(FIELD_JOB_SIGNATURE, t -> t.terms(terms -> terms.field(FIELD_JOB_SIGNATURE)))
                            .size(0)
                    ,
                    MAP_CLASS
            );
            final StringTermsAggregate terms = response
                    .aggregations()
                    .get(FIELD_JOB_SIGNATURE)
                    .sterms();

            return terms
                    .buckets()
                    .array()
                    .stream()
                    .map(StringTermsBucket::key)
                    .map(FieldValue::stringValue)
                    .collect(toSet());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean recurringJobExists(final String recurringJobId, final StateName... states) {
        try {
            final QueryVariant query = bool()
                    .must(shouldMatch(states))
                    .must(mu -> mu.match(ma -> ma.field(FIELD_RECURRING_JOB_ID).query(recurringJobId)))
                    .build();

            return countJobs(query) > 0;
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public RecurringJob saveRecurringJob(final RecurringJob job) {
        try {
            client.index(i -> i
                    .index(recurringJobIndexName)
                    .id(job.getId())
                    .document(documentMapper.toMap(job))
                    .refresh(True)
            );

            return job;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public RecurringJobsResult getRecurringJobs() {
        try {
            final SearchResponse<Map> response = client.search(
                    s -> s
                            .index(recurringJobIndexName)
                            .query(q -> q.matchAll(m -> m))
                            .storedFields(FIELD_JOB_AS_JSON)
                            .size(MAX_SIZE),
                    MAP_CLASS
            );

            final List<RecurringJob> jobs = response
                    .hits()
                    .hits()
                    .stream()
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
            final SearchResponse<?> response = client.search(
                    s -> s
                            .index(recurringJobIndexName)
                            .query(q -> q.matchAll(m -> m))
                            .aggregations(FIELD_CREATED_AT, aggs -> aggs.sum(sum -> sum.field(FIELD_CREATED_AT)))
                            .size(0),
                    MAP_CLASS
            );
            final SumAggregate parsedSum = response.aggregations().get(FIELD_CREATED_AT).sum();

            return !recurringJobsUpdatedHash.equals(Double.valueOf(parsedSum.value()).longValue());
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deleteRecurringJob(final String id) {
        try {
            final DeleteResponse response = client.delete(
                    d -> d
                            .index(recurringJobIndexName)
                            .id(id)
                            .refresh(True)
            );
            final int amountDeleted = response.shards().successful().intValue();

            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);

            return amountDeleted;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public JobStats getJobStats() {
        try {
            final SearchResponse<?> response = client.search(
                    s -> s
                            .index(jobIndexName)
                            .size(0)
                            .query(q -> q.matchAll(m -> m))
                            .aggregations(FIELD_STATE, aggs -> aggs.terms(t -> t.field(FIELD_STATE))),
                    MAP_CLASS
            );

            final StringTermsAggregate terms = response
                    .aggregations()
                    .get(FIELD_STATE)
                    .sterms();
            final List<StringTermsBucket> buckets = terms.buckets().array();

            final long allTimeSucceededJobs = getAllTimeSucceededJobs();
            final int recurringJobs = getCount(recurringJobIndexName);
            final int backgroundJobServers = getCount(backgroundJobServerIndexName);

            final Instant now = Instant.now();
            return new JobStats(
                    now,
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
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    private long getAllTimeSucceededJobs() throws IOException {
        final GetResponse<Map> response = client.get(
                g -> g
                        .index(metadataIndexName)
                        .id(STATS_ID),
                MAP_CLASS
        );
        final Object value = ofNullable(response.source())
                .orElse(EMPTY_MAP)
                .getOrDefault(FIELD_VALUE, 0L);

        return ((Number) value).longValue();
    }

    private int getCount(final String indexName) throws IOException {
        return (int) client.count(c -> c.index(indexName)).count();
    }

    private long count(final List<StringTermsBucket> buckets, final StateName state) {
        return buckets
                .stream()
                .filter(bucket -> Objects.equals(state.name(), bucket.key().stringValue()))
                .map(StringTermsBucket::docCount)
                .findFirst()
                .orElse(0L);
    }

    @Override
    public void publishTotalAmountOfSucceededJobs(final int amount) {
        try {
            final Map<String, JsonData> parameters = singletonMap("value", JsonData.of(amount));
            final Script script = Script.of(s -> s
                    .lang("painless")
                    .source("ctx._source." + FIELD_VALUE + " += params.value")
                    .params(parameters)
            );

            client.update(
                    u -> u
                            .index(metadataIndexName)
                            .id(STATS_ID)
                            .scriptedUpsert(true)
                            .script(script),
                    parameters.getClass()
            );
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    long countJobs(final QueryVariant query) throws IOException {
        final CountResponse response = client.count(
                c -> c
                        .index(jobIndexName)
                        .query(query._toQuery())
        );

        return response.count();
    }

    private List<Job> findJobs(final QueryVariant query, final AmountRequest amountRequest) {
        try {
            return client.search(
                            s -> s
                                    .index(jobIndexName)
                                    .query(query._toQuery())
                                    .from(amountRequest instanceof OffsetBasedPageRequest ? (int) ((OffsetBasedPageRequest) amountRequest).getOffset() : 0)
                                    .size(amountRequest.getLimit())
                                    .storedFields(Jobs.FIELD_JOB_AS_JSON)
                                    .sort(sortJobs(amountRequest)),
                            MAP_CLASS
                    ).hits().hits()
                    .stream()
                    .map(documentMapper::toJob)
                    .collect(toList());
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    private static Query shouldMatch(final StateName... states) {
        final BoolQuery.Builder query = new BoolQuery.Builder();

        for (final StateName state : states) {
            query.should(s -> s.match(m -> m.field(FIELD_STATE).query(state.toString())));
        }

        return query.build()._toQuery();
    }

    private static List<SortOptions> sortJobs(final AmountRequest amountRequest) {
        List<SortOptions> sortOptions = new ArrayList<>();
        List<OrderTerm> orderTerms = amountRequest.getAllOrderTerms(ALLOWED_SORT_COLUMNS.keySet());
        for (OrderTerm orderTerm : orderTerms) {
            sortOptions.add(
                    new SortOptions.Builder().field(
                            f -> f.field(orderTerm.getFieldName()).order(OrderTerm.Order.ASC == orderTerm.getOrder() ? Asc : Desc)
                    ).build()
            );
        }
        return sortOptions;
    }

    private static ElasticsearchClient newClient(final HttpHost host) {
        final RestClientBuilder builder = RestClient.builder(host);
        final JacksonJsonpMapper mapper = new JacksonJsonpMapper();
        final ElasticsearchTransport transport = new RestClientTransport(
                builder.build(),
                mapper
        );

        return new ElasticsearchClient(transport);
    }
}
