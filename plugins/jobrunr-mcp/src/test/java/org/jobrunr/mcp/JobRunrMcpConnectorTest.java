package org.jobrunr.mcp;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.server.dashboard.CpuAllocationIrregularityNotification;
import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;

class JobRunrMcpConnectorTest {

    private static final String REQUEST_ID = "11111111111111111111111111111111";

    private final BlockingQueue<String> requestsForConnector = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> resultsFromConnector = new LinkedBlockingQueue<>();
    private final AtomicInteger resultAttempts = new AtomicInteger();
    private volatile Headers pollHeaders;
    private volatile boolean failFirstResult;
    private StorageProvider storageProvider;
    private HttpServer hub;
    private JobRunrMcpConnector connector;

    @BeforeEach
    void startHub() throws IOException {
        storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));

        hub = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        hub.setExecutor(Executors.newCachedThreadPool());
        hub.createContext("/connector/poll", this::poll);
        hub.createContext("/connector/results/", this::result);
        hub.start();
    }

    @AfterEach
    void stopHub() {
        if (connector != null) connector.stop();
        hub.stop(0);
    }

    @Test
    void answersRequestsOfTheHubWithTheDashboardApi() throws InterruptedException {
        Job job = storageProvider.save(aFailedJobWithRetries().build());
        startConnector("jrc_read_token");

        requestsForConnector.add(REQUEST_ID + " GET /api/jobs/" + job.getId());

        assertThat(resultsFromConnector.poll(5, SECONDS)).startsWith(REQUEST_ID + " 200 {").contains(job.getId().toString());
        assertThat(pollHeaders.getFirst("Authorization")).isEqualTo("Bearer jrc_read_token");
        assertThat(pollHeaders.getFirst("X-JobRunr-Redaction")).isEqualTo("false");
    }

    @Test
    void readTokenCannotChangeJobs() throws InterruptedException {
        Job job = storageProvider.save(aFailedJobWithRetries().build());
        startConnector("jrc_read_token");

        requestsForConnector.add(REQUEST_ID + " POST /api/jobs/" + job.getId() + "/requeue");

        assertThat(resultsFromConnector.poll(5, SECONDS)).isEqualTo(REQUEST_ID + " 403 {\"error\":\"This connector token does not allow POST /api/jobs/" + job.getId() + "/requeue\"}");
        assertThat(storageProvider.getJobById(job.getId())).hasState(FAILED);
    }

    @Test
    void operateTokenCanRequeueJobs() throws InterruptedException {
        Job job = storageProvider.save(aFailedJobWithRetries().build());
        startConnector("jrc_operate_token");

        requestsForConnector.add(REQUEST_ID + " POST /api/jobs/" + job.getId() + "/requeue");

        assertThat(resultsFromConnector.poll(5, SECONDS)).isEqualTo(REQUEST_ID + " 204 ");
        assertThat(storageProvider.getJobById(job.getId())).hasState(ENQUEUED);
    }

    @Test
    void readTokenCannotGetArbitraryMetadata() throws InterruptedException {
        storageProvider.saveMetadata(new JobRunrMetadata("credentials", "app", "{\"password\":\"SENTINEL\"}"));
        startConnector("jrc_read_token");

        requestsForConnector.add(REQUEST_ID + " GET /api/metadata/credentials/app?format=jsonValue");

        assertThat(resultsFromConnector.poll(5, SECONDS)).startsWith(REQUEST_ID + " 403 ");
    }

    @Test
    void readingProblemsDoesNotDeleteStoredPollIntervalWarning() throws InterruptedException {
        storageProvider.saveMetadata(new JobRunrMetadata(
                CpuAllocationIrregularityNotification.class.getSimpleName(), "server-1", "23"));
        storageProvider.saveMetadata(new JobRunrMetadata(
                PollIntervalInSecondsTimeBoxIsTooSmallNotification.class.getSimpleName(), "server-1", "5"));
        startConnector("jrc_read_token");

        requestsForConnector.add(REQUEST_ID + " GET /api/problems");

        assertThat(resultsFromConnector.poll(5, SECONDS)).startsWith(REQUEST_ID + " 200 ");
        assertThat(storageProvider.getMetadata(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class.getSimpleName())).hasSize(1);
    }

    @Test
    void retriesTransientResultRejectionWithTheSameRequestId() throws InterruptedException {
        failFirstResult = true;
        startConnector("jrc_read_token");
        requestsForConnector.add(REQUEST_ID + " GET /api/version");

        assertThat(resultsFromConnector.poll(5, SECONDS)).startsWith(REQUEST_ID + " 200 ");
        assertThat(resultsFromConnector.poll(5, SECONDS)).startsWith(REQUEST_ID + " 200 ");
        assertThat(resultAttempts.get()).isEqualTo(2);
    }

    @Test
    void rejectsPlaintextRemoteHubUrls() {
        assertThatThrownBy(() -> new JobRunrMcpConnector(
                "jrc_read_token", "http://example.invalid", storageProvider, new JacksonJsonMapper()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
        assertThat(new JobRunrMcpConnector(
                "jrc_read_token", "http://[::1]:18080", storageProvider, new JacksonJsonMapper()))
                .isNotNull();
    }

    private void startConnector(String token) {
        connector = new JobRunrMcpConnector(token, "http://127.0.0.1:" + hub.getAddress().getPort(), storageProvider, new JacksonJsonMapper());
        connector.start();
    }

    private void poll(HttpExchange exchange) throws IOException {
        pollHeaders = exchange.getRequestHeaders();
        try {
            String request = requestsForConnector.poll(100, MILLISECONDS);
            respond(exchange, request == null ? 204 : 200, request == null ? "" : request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void result(HttpExchange exchange) throws IOException {
        String requestId = exchange.getRequestURI().getPath().substring("/connector/results/".length());
        String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
        resultsFromConnector.add(requestId + " " + exchange.getRequestHeaders().getFirst("X-JobRunr-Status") + " " + body);
        int attempt = resultAttempts.incrementAndGet();
        respond(exchange, failFirstResult && attempt == 1 ? 503 : 204, "");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
