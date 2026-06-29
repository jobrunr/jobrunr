package org.jobrunr.server.costaware;

import org.jobrunr.server.costaware.CostAwareScaleUpRequest.InstanceEnvironment;
import org.jobrunr.server.costaware.CostAwareScaleUpRequest.InstanceSpecifications;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CostAwareApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CostAwareApiClient.class);
    private final HttpClient httpClient;
    private final CostAwareConfigurationReader costAwareConfigurationReader;
    private final JsonMapper jsonMapper;

    // TODO why is this not an allowlist instead of blocklist? Which variables are we using?
    private static final Set<String> BLOCKED_ENV_VARIABLES = Set.of(
            "PATH", "USER", "LOGNAME", "HOME", "SHELL", "TMPDIR", "PWD", "OLDPWD",
            "DEBUGGER_ID", "DEBUGGER_ENABLED", "PROCESS_OPTIONS", "PROCESS_PARAMETERS",
            "COMMAND_MODE", "SSH_AUTH_SOCK", "XPC_FLAGS", "XPC_SERVICE_NAME", "INFOPATH", "FPATH"
    );

    // TODO why is this not an allowlist instead of blocklist? Which variables are we using?
    private static final List<String> BLOCKED_ENV_VARIABLE_PREFIXES = List.of(
            "HOMEBREW_", "IDEA_", "JAVA_", "GOTOOLCHAIN", "TERMINAL_",
            "ALLUSERSPROFILE", "APPDATA", "COMPUTERNAME", "ProgramFiles", "SystemRoot"
    ); // I don't like how hard coded these are, is there a better way to do this?


    public CostAwareApiClient(CostAwareConfigurationReader costAwareConfiguration, JsonMapper jsonMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.costAwareConfigurationReader = costAwareConfiguration;
        this.jsonMapper = jsonMapper;
    }

    public void scaleUp(String clusterId) throws CostAwareApiClientException {
        try {
            // TODO method is too long, let's extract the below setup into a `getCostAwareScaleUpRequest`
            Map<String, String> allEnvironmentVariables = costAwareConfigurationReader.getAdditionalEnvironmentVariables();
            if (costAwareConfigurationReader.isUseCurrentEnvironmentVariables()) {
                allEnvironmentVariables.putAll(System.getenv());
            }

            allEnvironmentVariables = allEnvironmentVariables.entrySet().stream()
                    .filter(entry -> !BLOCKED_ENV_VARIABLES.contains(entry.getKey()))
                    .filter(entry -> BLOCKED_ENV_VARIABLE_PREFIXES.stream().noneMatch(entry.getKey()::startsWith))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)); // why? filter out system specific environment

            CostAwareScaleUpRequest scaleUpDto = new CostAwareScaleUpRequest(
                    costAwareConfigurationReader.getProviderConfiguration().getProvider(),
                    costAwareConfigurationReader.getRegions(),
                    clusterId,
                    costAwareConfigurationReader.getProviderConfiguration().asMap(),
                    new InstanceSpecifications(
                            costAwareConfigurationReader.getVcpuCores(),
                            costAwareConfigurationReader.getMemoryGiB(),
                            costAwareConfigurationReader.getGpuMemoryGiB()
                    ),
                    new InstanceEnvironment(
                            costAwareConfigurationReader.getDockerImage(),
                            costAwareConfigurationReader.getImageArchitecture(),
                            allEnvironmentVariables
                    )
            );

            // TODO timeouts (similar to carbon aware)
            HttpRequest request = HttpRequest.newBuilder(URI.create(CostAwareConfiguration.COST_AWARE_API_URL + "/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.serialize(scaleUpDto)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                // TODO because the exception is caught and logged, we double the logging
                LOGGER.warn("JobRunr was unable to provision a spot instance");
                throw new CostAwareApiClientException("Error scaling up, status code " + response.statusCode());
            } else {
                // TODO wouldn't this log too much?
                LOGGER.info("JobRunr has provisioned a spot instance, waiting for it to be active");
            }
        } catch (IOException | InterruptedException e) {
            throw new CostAwareApiClientException("Error scaling up", e);
        }
    }

    public void scaleDown(String clusterId) throws CostAwareApiClientException {
        try {
            CostAwareScaleDownRequest scaleDownDto = new CostAwareScaleDownRequest(
                    clusterId,
                    costAwareConfigurationReader.getProviderConfiguration().asMap()
            );

            HttpRequest request = HttpRequest.newBuilder(URI.create(CostAwareConfiguration.COST_AWARE_API_URL + "/delete"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.serialize(scaleDownDto)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.warn("JobRunr failed to scale down");
                throw new CostAwareApiClientException("Error scaling down, status code " + response.statusCode());
            } else {
                LOGGER.info("JobRunr has scaled down, waiting for instance to shutdown");
            }
        } catch (IOException | InterruptedException e) {
            throw new CostAwareApiClientException("Error scaling down", e);
        }
    }

    // TODO I don't think this belongs here, the task is disabled not the client
    public boolean isDisabled() {
        return !costAwareConfigurationReader.isEnabled();
    }
}
