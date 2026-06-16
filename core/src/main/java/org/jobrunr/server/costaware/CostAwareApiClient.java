package org.jobrunr.server.costaware;

import org.jobrunr.server.costaware.CostAwareScaleUpDto.InstanceEnvironment;
import org.jobrunr.server.costaware.CostAwareScaleUpDto.InstanceSpecifications;
import org.jobrunr.server.tasks.zookeeper.CostAwareManagementTask.SpotScalingMetadata;
import org.jobrunr.server.tasks.zookeeper.CostAwareManagementTask.SpotScalingMetadata.ScalingStatus;
import org.jobrunr.storage.StorageProvider;
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
    private final StorageProvider storageProvider;

    private static final Set<String> BLOCKED_ENV_VARIABLES = Set.of(
            "PATH", "USER", "LOGNAME", "HOME", "SHELL", "TMPDIR", "PWD", "OLDPWD",
            "DEBUGGER_ID", "DEBUGGER_ENABLED", "PROCESS_OPTIONS", "PROCESS_PARAMETERS",
            "COMMAND_MODE", "SSH_AUTH_SOCK", "XPC_FLAGS", "XPC_SERVICE_NAME", "INFOPATH", "FPATH"
    );

    private static final List<String> BLOCKED_ENV_VARIABLE_PREFIXES = List.of(
            "HOMEBREW_", "IDEA_", "JAVA_", "GOTOOLCHAIN", "TERMINAL_",
            "ALLUSERSPROFILE", "APPDATA", "COMPUTERNAME", "ProgramFiles", "SystemRoot"
    ); // I don't like how hard coded these are, is there a better way to do this?


    public CostAwareApiClient(CostAwareConfigurationReader costAwareConfiguration, JsonMapper jsonMapper, StorageProvider storageProvider) {
        this.httpClient = HttpClient.newHttpClient();
        this.costAwareConfigurationReader = costAwareConfiguration;
        this.jsonMapper = jsonMapper;
        this.storageProvider = storageProvider;
    }

    public void scaleUp(String clusterId) throws CostAwareApiClientException {
        try {
            Map<String, String> allEnvironmentVariables = costAwareConfigurationReader.getAdditionalEnvironmentVariables();
            if (costAwareConfigurationReader.isUseCurrentEnvironmentVariables()) {
                allEnvironmentVariables.putAll(System.getenv());
            }

            allEnvironmentVariables = allEnvironmentVariables.entrySet().stream()
                    .filter(entry -> !BLOCKED_ENV_VARIABLES.contains(entry.getKey()))
                    .filter(entry -> BLOCKED_ENV_VARIABLE_PREFIXES.stream().noneMatch(entry.getKey()::startsWith))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)); // why? filter out system specific environment

            CostAwareScaleUpDto scaleUpDto = new CostAwareScaleUpDto(
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

            HttpRequest request = HttpRequest.newBuilder(URI.create(CostAwareConfiguration.COST_AWARE_API_URL + "/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.serialize(scaleUpDto)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            SpotScalingMetadata scalingMetadata = SpotScalingMetadata.readMetadata(storageProvider, jsonMapper);
            if (response.statusCode() != 200) {
                if (scalingMetadata != null) {
                    scalingMetadata.setScalingStatus(ScalingStatus.FAILED);
                    scalingMetadata.saveToSpotScalingMetadata(storageProvider, jsonMapper);
                }
                LOGGER.warn("JobRunr was unable to provision a spot instance");
                throw new CostAwareApiClientException("Error scaling up, status code " + response.statusCode());
            } else {
                if (scalingMetadata != null) {
                    scalingMetadata.setScalingStatus(ScalingStatus.PROVISIONED);
                    scalingMetadata.saveToSpotScalingMetadata(storageProvider, jsonMapper);
                }
                LOGGER.info("JobRunr has provisioned a spot instance, waiting for it to be active");
            }
        } catch (IOException | InterruptedException e) {
            throw new CostAwareApiClientException("Error scaling up", e);
        }
    }

    public void scaleDown(String clusterId) throws CostAwareApiClientException {
        try {
            CostAwareScaleDownDto scaleDownDto = new CostAwareScaleDownDto(
                    clusterId,
                    costAwareConfigurationReader.getProviderConfiguration().asMap()
            );

            HttpRequest request = HttpRequest.newBuilder(URI.create(CostAwareConfiguration.COST_AWARE_API_URL + "/delete"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.serialize(scaleDownDto)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            SpotScalingMetadata scalingMetadata = SpotScalingMetadata.readMetadata(storageProvider, jsonMapper);
            if (response.statusCode() != 200) {
                if (scalingMetadata != null) {
                    scalingMetadata.setScalingStatus(ScalingStatus.FAILED);
                    scalingMetadata.saveToSpotScalingMetadata(storageProvider, jsonMapper);
                }
                LOGGER.warn("JobRunr failed to scale down");
                throw new CostAwareApiClientException("Error scaling down, status code " + response.statusCode());
            } else {
                if (scalingMetadata != null) {
                    scalingMetadata.setScalingStatus(ScalingStatus.SCALED_DOWN);
                    scalingMetadata.saveToSpotScalingMetadata(storageProvider, jsonMapper);
                }
                LOGGER.info("JobRunr has scaled down, waiting for instance to shutdown");
            }
        } catch (IOException | InterruptedException e) {
            throw new CostAwareApiClientException("Error scaling down", e);
        }
    }

    public boolean isDisabled() {
        return !costAwareConfigurationReader.isEnabled();
    }
}
