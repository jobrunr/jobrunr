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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
 * Responses include job parameters and logs as stored. The OSS connector does not redact them.
 */
public class JobRunrMcpConnector {

    public static final String DEFAULT_HUB_URL = "https://mcp.jobrunr.io";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrMcpConnector.class);
    private static final Pattern REQUEST_ID = Pattern.compile("[0-9a-fA-F]{32}");
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_POLL_RESPONSE_BYTES = 64 * 1024;
    private static final int MAX_POLL_LINES = 64;
    private static final int MAX_POLL_LINE_LENGTH = 768;
    private static final int MAX_QUEUED_REQUESTS = 32;
    private static final int MAX_ERROR_LENGTH = 500;
    private static final long MAX_RETRY_DELAY_MS = 60_000;
    private static final long REFUSED_RETRY_DELAY_MS = 5 * 60_000;

    private final String token;
    private final boolean canOperate;
    private final URI hubUri;
    private final JobRunrApiHandler apiHandler;
    private final String nodeId = UUID.randomUUID().toString();
    private final String version = String.valueOf(JarUtils.getVersion(JobRunr.class));
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final HttpRequest pollRequest;
    private final ExecutorService executorService = new ThreadPoolExecutor(
            3, 3, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(MAX_QUEUED_REQUESTS),
            new NamedThreadFactory("jobrunr-mcp-connector", true), new ThreadPoolExecutor.CallerRunsPolicy());
    private boolean connected;

    public JobRunrMcpConnector(String token, StorageProvider storageProvider) {
        this(token, DEFAULT_HUB_URL, storageProvider, JsonMapperFactory.createJsonMapper());
    }

    public JobRunrMcpConnector(String token, String hubUrl, StorageProvider storageProvider, JsonMapper jsonMapper) {
        if (token == null || !(token.startsWith("jrc_read_") || token.startsWith("jrc_operate_"))) {
            throw new IllegalArgumentException("A JobRunr MCP connector token is required");
        }
        this.token = token;
        this.canOperate = token.startsWith("jrc_operate_");
        this.hubUri = validateHubUri(hubUrl);
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
                if (retryDelayMs == 0) LOGGER.warn("JobRunr MCP connector cannot reach {}, retrying: {}", hubUri, e.getClass().getSimpleName());
                connected = false;
                retryDelayMs = Math.min(Math.max(1_000, retryDelayMs * 2), MAX_RETRY_DELAY_MS);
            } catch (RuntimeException e) {
                LOGGER.warn("JobRunr MCP connector poll failed with {}", e.getClass().getSimpleName());
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
        HttpResponse<byte[]> response = httpClient.send(pollRequest, LimitedBodySubscriber.handler(MAX_POLL_RESPONSE_BYTES));
        if (response.statusCode() / 100 == 4) {
            // a revoked token or a second cluster needs the user to act, so there is no point in retrying soon
            LOGGER.warn("JobRunr MCP server refused the connector (HTTP {}), retrying in 5 minutes", response.statusCode());
            connected = false;
            return REFUSED_RETRY_DELAY_MS;
        }
        if (response.statusCode() / 100 != 2) throw new IOException("HTTP " + response.statusCode());
        if (!connected) LOGGER.info("JobRunr MCP connector is connected to {} with {} access", hubUri, canOperate ? "read and operate" : "read-only");
        connected = true;
        List<PollRequest> requests = parsePollLines(new String(response.body(), StandardCharsets.UTF_8));
        for (PollRequest request : requests) {
            executorService.execute(() -> {
                if (request.malformed()) postResult(request.id(), 400, error("Malformed MCP tunnel request"));
                else answer(request.id(), request.method(), request.path());
            });
        }
        // The normal hub holds an empty poll. A misconfigured hub can answer 204 immediately.
        return requests.isEmpty() ? ThreadLocalRandom.current().nextLong(250, 751) : 0;
    }

    static List<PollRequest> parsePollLines(String body) throws IOException {
        List<String> lines = body.lines().limit(MAX_POLL_LINES + 1L).toList();
        if (lines.size() > MAX_POLL_LINES) throw new IOException("MCP poll contains too many request lines");
        List<PollRequest> requests = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (line.isBlank()) continue;
            int firstSpace = line.indexOf(' ');
            String id = firstSpace < 0 ? line : line.substring(0, firstSpace);
            if (!REQUEST_ID.matcher(id).matches()) {
                LOGGER.warn("MCP hub returned a request line with an invalid id");
                continue;
            }
            int secondSpace = firstSpace < 0 ? -1 : line.indexOf(' ', firstSpace + 1);
            if (secondSpace < 0 || line.length() > MAX_POLL_LINE_LENGTH) {
                requests.add(new PollRequest(id, null, null, true));
                continue;
            }
            String method = line.substring(firstSpace + 1, secondSpace);
            String path = line.substring(secondSpace + 1);
            boolean malformed = method.isBlank() || path.isBlank() || path.indexOf(' ') >= 0
                    || path.chars().anyMatch(Character::isISOControl);
            requests.add(new PollRequest(id, method, path, malformed));
        }
        return requests;
    }

    record PollRequest(String id, String method, String path, boolean malformed) {
    }

    private void answer(String requestId, String method, String path) {
        if (!isAllowed(method, path)) {
            postResult(requestId, 403, error("This connector token does not allow " + method + " " + path));
            return;
        }
        InMemoryHttpExchange exchange = new InMemoryHttpExchange(method, path);
        try {
            apiHandler.handle(exchange);
        } catch (RuntimeException e) {
            if (exchange.responseExceededLimit()) {
                postResult(requestId, 502, error("Dashboard response exceeded the MCP connector limit"));
            } else {
                LOGGER.warn("Dashboard MCP request failed with {}", e.getClass().getSimpleName());
                postResult(requestId, 502, error("Dashboard request failed"));
            }
            return;
        }
        if (exchange.responseExceededLimit()) {
            postResult(requestId, 502, error("Dashboard response exceeded the MCP connector limit"));
            return;
        }
        String body = exchange.getResponseBodyAsString();
        // the dashboard answers a failure with a stack trace, of which only the first line is useful to an agent
        postResult(requestId, exchange.getResponseCode(), exchange.getResponseCode() == 500
                ? error(truncate(body.lines().findFirst().orElse(""), MAX_ERROR_LENGTH)) : body);
    }

    private boolean isAllowed(String method, String path) {
        return DashboardRequestPolicy.isAllowed(method, path, canOperate);
    }

    private void postResult(String requestId, int status, String body) {
        HttpRequest request = hubRequest("/connector/results/" + requestId)
                .header("Content-Type", "application/json")
                .header("X-JobRunr-Status", String.valueOf(status))
                .POST(BodyPublishers.ofString(body))
                .build();
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpResponse<Void> response = httpClient.send(request, BodyHandlers.discarding());
                if (response.statusCode() / 100 == 2 || response.statusCode() == 404) return;
                if (response.statusCode() / 100 == 4) {
                    LOGGER.warn("JobRunr MCP result {} was refused (HTTP {})", requestId, response.statusCode());
                    return;
                }
                if (attempt == 3) LOGGER.warn("JobRunr MCP result {} failed after 3 attempts (HTTP {})", requestId, response.statusCode());
            } catch (IOException e) {
                if (attempt == 3) LOGGER.warn("JobRunr MCP result {} failed after 3 attempts ({})", requestId, e.getClass().getSimpleName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                Thread.sleep(200L * attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
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
        StringBuilder json = new StringBuilder("{\"error\":\"");
        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            switch (ch) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (ch < 0x20) json.append(String.format("\\u%04x", (int) ch));
                    else json.append(ch);
                }
            }
        }
        return json.append("\"}").toString();
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static URI validateHubUri(String hubUrl) {
        URI uri = URI.create(hubUrl);
        String scheme = uri.getScheme();
        boolean loopback = "localhost".equalsIgnoreCase(uri.getHost())
                || "127.0.0.1".equals(uri.getHost())
                || "[::1]".equals(uri.getHost());
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !(uri.getRawPath() == null || uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath()))
                || !("https".equalsIgnoreCase(scheme) || ("http".equalsIgnoreCase(scheme) && loopback))) {
            throw new IllegalArgumentException("JobRunr MCP hub URL must be HTTPS (HTTP is allowed only on loopback)");
        }
        return uri;
    }

    private static String framework() {
        if (classExists("org.springframework.boot.SpringApplication")) return "spring-boot";
        if (classExists("io.quarkus.runtime.Quarkus")) return "quarkus";
        if (classExists("io.micronaut.runtime.Micronaut")) return "micronaut";
        return "plain-java";
    }
}
