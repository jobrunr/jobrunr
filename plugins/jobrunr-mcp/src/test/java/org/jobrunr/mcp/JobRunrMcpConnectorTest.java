package org.jobrunr.mcp;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.InMemoryStorageProvider;
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;

class JobRunrMcpConnectorTest {

    private final BlockingQueue<String> requestsForConnector = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> resultsFromConnector = new LinkedBlockingQueue<>();
    private volatile Headers pollHeaders;
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
        connector.stop();
        hub.stop(0);
    }

    @Test
    void answersRequestsOfTheHubWithTheDashboardApi() throws InterruptedException {
        Job job = storageProvider.save(aFailedJobWithRetries().build());
        startConnector("jrc_read_token");

        requestsForConnector.add("r1 GET /api/jobs/" + job.getId());

        assertThat(resultsFromConnector.poll(5, SECONDS)).startsWith("r1 200 {").contains(job.getId().toString());
        assertThat(pollHeaders.getFirst("Authorization")).isEqualTo("Bearer jrc_read_token");
        assertThat(pollHeaders.getFirst("X-JobRunr-Redaction")).isEqualTo("false");
    }

    @Test
    void readTokenCannotChangeJobs() throws InterruptedException {
        Job job = storageProvider.save(aFailedJobWithRetries().build());
        startConnector("jrc_read_token");

        requestsForConnector.add("r1 POST /api/jobs/" + job.getId() + "/requeue");

        assertThat(resultsFromConnector.poll(5, SECONDS)).isEqualTo("r1 403 {\"error\":\"This connector token does not allow POST /api/jobs/" + job.getId() + "/requeue\"}");
        assertThat(storageProvider.getJobById(job.getId())).hasState(FAILED);
    }

    @Test
    void operateTokenCanRequeueJobs() throws InterruptedException {
        Job job = storageProvider.save(aFailedJobWithRetries().build());
        startConnector("jrc_operate_token");

        requestsForConnector.add("r1 POST /api/jobs/" + job.getId() + "/requeue");

        assertThat(resultsFromConnector.poll(5, SECONDS)).isEqualTo("r1 204 ");
        assertThat(storageProvider.getJobById(job.getId())).hasState(ENQUEUED);
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
        respond(exchange, 204, "");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
