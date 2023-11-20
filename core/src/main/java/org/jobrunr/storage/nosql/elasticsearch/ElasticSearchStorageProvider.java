package org.jobrunr.storage.nosql.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.util.ObjectBuilder;
import org.apache.http.HttpHost;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
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

import static co.elastic.clients.elasticsearch._types.Refresh.True;
import static co.elastic.clients.elasticsearch._types.ScriptBuilders.inline;
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
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.storage.JobRunrMetadata.toId;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.*;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.CREATE;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.*;
import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.storage.StorageProviderUtils.Metadata.FIELD_VALUE;
import static org.jobrunr.storage.StorageProviderUtils.Metadata.STATS_ID;
import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs.FIELD_CREATED_AT;
import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs.FIELD_JOB_AS_JSON;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.JOBRUNR_PREFIX;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

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
                        r -> r
                                .field(FIELD_LAST_HEARTBEAT)
                                .to(Long.toString(heartbeatOlderThan.toEpochMilli()))
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
    public List<Job> save(final List<Job> jobs) {
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
    public List<Job> getJobs(final StateName state, final Instant updatedBefore, final PageRequest pageRequest) {
        try {
            final QueryVariant query = withStateAndUpdatedBefore(state, updatedBefore);

            final SearchResponse<Map> response = searchJobs(query, pageRequest);
            return response
                    .hits()
                    .hits()
                    .stream()
                    .map(documentMapper::toJob)
                    .collect(toList());
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getScheduledJobs(final Instant scheduledBefore, final PageRequest pageRequest) {
        try {
            final QueryVariant query =
                    QueryBuilders.range()
                            .field(FIELD_SCHEDULED_AT)
                            .to(Long.toString(scheduledBefore.toEpochMilli()))
                            .build();

            final SearchResponse<Map> response = searchJobs(query, pageRequest);
            return response
                    .hits()
                    .hits()
                    .stream()
                    .map(documentMapper::toJob)
                    .collect(toList());
        } catch (final IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getJobs(final StateName state, final PageRequest pageRequest) {
        try {
            final QueryVariant query = bool()
                    .must(must -> must.match(match -> match.field(FIELD_STATE).query(state.toString())))
                    .build();

            final SearchResponse<Map> response = searchJobs(query, pageRequest);

            return response
                    .hits()
                    .hits()
                    .stream()
                    .map(documentMapper::toJob)
                    .collect(toList());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        try {
            final QueryVariant query = bool()
                    .must(must -> must.match(match -> match.field(FIELD_STATE).query(state.toString())))
                    .build();

            long count = countJobs(query);
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
                .must(must -> must.range(m -> m.field(FIELD_UPDATED_AT).to(Long.toString(updatedBefore.toEpochMilli()))))
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
    public boolean exists(final JobDetails jobDetails, final StateName... states) {
        try {
            final QueryVariant query = bool()
                    .must(shouldMatch(states))
                    .must(must -> must.match(match -> match.field(FIELD_JOB_SIGNATURE).query(getJobSignature(jobDetails))))
                    .build();

            return countJobs(query) > 0;
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
            final InlineScript inline = inline()
                    .lang("painless")
                    .source("ctx._source." + FIELD_VALUE + " += params.value")
                    .params(parameters)
                    .build();

            client.update(
                    u -> u
                            .index(metadataIndexName)
                            .id(STATS_ID)
                            .scriptedUpsert(true)
                            .script(s -> s.inline(inline)),
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

    SearchResponse<Map> searchJobs(
            final QueryVariant query,
            final PageRequest page) throws IOException {

        return client.search(
                s -> s
                        .index(jobIndexName)
                        .query(query._toQuery())
                        .from((int) page.getOffset())
                        .size(page.getLimit())
                        .storedFields(Jobs.FIELD_JOB_AS_JSON)
                        .sort(sort -> sortJobs(page, sort)),
                MAP_CLASS
        );
    }

    private static Query shouldMatch(final StateName... states) {
        final BoolQuery.Builder query = new BoolQuery.Builder();

        for (final StateName state : states) {
            query.should(s -> s.match(m -> m.field(FIELD_STATE).query(state.toString())));
        }

        return query.build()._toQuery();
    }

    private static ObjectBuilder<SortOptions> sortJobs(final PageRequest page, final SortOptions.Builder s) {
        final String order = page.getOrder();

        final SortOrder o;
        if (Objects.equals(order, "updatedAt:ASC")) {
            o = Asc;
        } else if (Objects.equals(order, "updatedAt:DESC")) {
            o = Desc;
        } else {
            throw new IllegalArgumentException("Unknown sort: " + order);
        }

        return s
                .field(f -> f.field(FIELD_UPDATED_AT)
                        .order(o));
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