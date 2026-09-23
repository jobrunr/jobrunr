package org.jobrunr.mcp;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.dashboard.JobRunrApiHandler;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.ThreadSafeStorageProvider;
import org.jobrunr.utils.JarUtils;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.JsonMapperFactory;
import org.jobrunr.utils.threadpool.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;

/**
 * Connects your JobRunr cluster to the JobRunr MCP server, so AI agents can inspect and operate your jobs:
 * <pre>
 * new JobRunrMcpConnector("jrc_...", storageProvider).start();
 * </pre>
 * The connection is outbound only: the connector long-polls the MCP server for dashboard API requests, answers them with the
 * {@link JobRunrApiHandler} of the dashboard and posts the result back. Agents can always read; requeueing, deleting and triggering
 * jobs and dismissing problems needs a connector token with operate access.
 */
public class JobRunrMcpConnector {

    public static final String DEFAULT_HUB_URL = "https://mcp.jobrunr.io";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrMcpConnector.class);
    private static final Pattern OPERATIONS = Pattern.compile("POST /api/jobs/[^/]+/requeue|DELETE /api/jobs/[^/]+|POST /api/recurring-jobs/[^/]+/trigger|DELETE /api/problems/[^/]+");
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final long MAX_RETRY_DELAY_MS = 60_000;
    private static final long REFUSED_RETRY_DELAY_MS = 5 * 60_000;

    private final String token;
    private final boolean canOperate;
    private final URI hubUri;
    private final JobRunrApiHandler apiHandler;
    private final String nodeId = UUID.randomUUID().toString();
    private final String version = String.valueOf(JarUtils.getVersion(JobRunr.class));
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final HttpRequest pollRequest;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3, new NamedThreadFactory("jobrunr-mcp-connector", true));
    private boolean connected;

    public JobRunrMcpConnector(String token, StorageProvider storageProvider) {
        this(token, DEFAULT_HUB_URL, storageProvider, JsonMapperFactory.createJsonMapper());
    }

    public JobRunrMcpConnector(String token, String hubUrl, StorageProvider storageProvider, JsonMapper jsonMapper) {
        this.token = token;
        this.canOperate = token.startsWith("jrc_operate_");
        this.hubUri = URI.create(hubUrl);
        // anonymous data usage adds the cluster id and storage provider type to /api/version, which the hub shows on the account page
        this.apiHandler = new JobRunrApiHandler(new ThreadSafeStorageProvider(storageProvider), jsonMapper, true);
        this.pollRequest = hubRequest("/connector/poll")
                .header("X-JobRunr-Version", version)
                .header("X-JobRunr-Java-Version", System.getProperty("java.version"))
                .header("X-JobRunr-Framework", framework())
                .header("X-JobRunr-Redaction", "false")
                .build();
    }

    public void start() {
        executorService.execute(this::pollHub);
    }

    public void stop() {
        executorService.shutdownNow();
    }

    private void pollHub() {
        long retryDelayMs = 0;
        while (!executorService.isShutdown()) {
            try {
                Thread.sleep(retryDelayMs);
                retryDelayMs = poll();
            } catch (IOException e) {
                if (retryDelayMs == 0) LOGGER.warn("JobRunr MCP connector cannot reach {}, retrying: {}", hubUri, e.toString());
                connected = false;
                retryDelayMs = Math.min(Math.max(1_000, retryDelayMs * 2), MAX_RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Hands the requests of one long-poll to the worker threads and returns how long to wait before polling again.
     */
    private long poll() throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(pollRequest, BodyHandlers.ofString());
        if (response.statusCode() / 100 == 4) {
            // a revoked token or a second cluster needs the user to act, so there is no point in retrying soon
            LOGGER.warn("JobRunr MCP server refused the connector (HTTP {}), retrying in 5 minutes: {}", response.statusCode(), response.body());
            connected = false;
            return REFUSED_RETRY_DELAY_MS;
        }
        if (response.statusCode() / 100 != 2) throw new IOException("HTTP " + response.statusCode());
        if (!connected) LOGGER.info("JobRunr MCP connector is connected to {} with {} access", hubUri, canOperate ? "read and operate" : "read-only");
        connected = true;
        response.body().lines()
                .filter(line -> !line.isBlank())
                .map(line -> line.split(" ", 3)) // request id, method and path
                .forEach(request -> executorService.execute(() -> answer(request[0], request[1], request[2])));
        return 0;
    }

    private void answer(String requestId, String method, String path) {
        if (!isAllowed(method, path)) {
            postResult(requestId, 403, error("This connector token does not allow " + method + " " + path));
            return;
        }
        InMemoryHttpExchange exchange = new InMemoryHttpExchange(method, path);
        apiHandler.handle(exchange);
        String body = exchange.getResponseBodyAsString();
        // the dashboard answers a failure with a stack trace, of which only the first line is useful to an agent
        postResult(requestId, exchange.getResponseCode(), exchange.getResponseCode() == 500 ? error(body.lines().findFirst().orElse("")) : body);
    }

    private boolean isAllowed(String method, String path) {
        return "GET".equals(method) || (canOperate && OPERATIONS.matcher(method + " " + path).matches());
    }

    private void postResult(String requestId, int status, String body) {
        try {
            httpClient.send(hubRequest("/connector/results/" + requestId)
                    .header("Content-Type", "application/json")
                    .header("X-JobRunr-Status", String.valueOf(status))
                    .POST(BodyPublishers.ofString(body))
                    .build(), BodyHandlers.discarding());
        } catch (IOException e) {
            LOGGER.debug("JobRunr MCP connector could not post the result of request {}", requestId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private HttpRequest.Builder hubRequest(String path) {
        return HttpRequest.newBuilder(hubUri.resolve(path))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "JobRunr MCP connector " + version)
                .header("X-JobRunr-Node", nodeId);
    }

    private static String error(String message) {
        return "{\"error\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    private static String framework() {
        if (classExists("org.springframework.boot.SpringApplication")) return "spring-boot";
        if (classExists("io.quarkus.runtime.Quarkus")) return "quarkus";
        if (classExists("io.micronaut.runtime.Micronaut")) return "micronaut";
        return "plain-java";
    }
}
