package org.jobrunr.server.costaware;

import org.jobrunr.server.costaware.CostAwareScaleUpDto.InstanceEnvironment;
import org.jobrunr.server.costaware.CostAwareScaleUpDto.InstanceSpecifications;
import org.jobrunr.utils.mapper.JsonMapper;

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

    private final HttpClient httpClient;
    private final CostAwareConfigurationReader costAwareConfigurationReader;
    private final JsonMapper jsonMapper;

    private static final Set<String> BLOCKED_ENV_VARIABLES = Set.of(
            "PATH", "USER", "LOGNAME", "HOME", "SHELL", "TMPDIR", "PWD", "OLDPWD",
            "DEBUGGER_ID", "DEBUGGER_ENABLED", "PROCESS_OPTIONS", "PROCESS_PARAMETERS",
            "COMMAND_MODE", "SSH_AUTH_SOCK", "XPC_FLAGS", "XPC_SERVICE_NAME", "INFOPATH", "FPATH"
    );

    private static final List<String> BLOCKED_ENV_VARIABLE_PREFIXES = List.of(
            "HOMEBREW_", "IDEA_", "JAVA_", "GOTOOLCHAIN", "TERMINAL_",
            "ALLUSERSPROFILE", "APPDATA", "COMPUTERNAME", "ProgramFiles", "SystemRoot"
    );

    public CostAwareApiClient(CostAwareConfigurationReader costAwareConfiguration, JsonMapper jsonMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.costAwareConfigurationReader = costAwareConfiguration;
        this.jsonMapper = jsonMapper;
    }

    public void scaleUp(String clusterId) throws CostAwareApiClientException {
        try {
            Map<String, String> allEnvironmentVariables = costAwareConfigurationReader.getAdditionalEnvironmentVariables();
            allEnvironmentVariables.putAll(System.getenv());

            allEnvironmentVariables = allEnvironmentVariables.entrySet().stream()
                    .filter(entry -> !BLOCKED_ENV_VARIABLES.contains(entry.getKey()))
                    .filter(entry -> BLOCKED_ENV_VARIABLE_PREFIXES.stream().noneMatch(entry.getKey()::startsWith))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

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
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // if anything but 200: CostAwareApiClientException
            if (response.statusCode() != 200) {
                throw new CostAwareApiClientException("Error scaling up, status code " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new CostAwareApiClientException("Error scaling up", e);
        }
    }

    public void scaleDown() {

    }

    public boolean isDisabled() {
        return !costAwareConfigurationReader.isEnabled();
    }
}
