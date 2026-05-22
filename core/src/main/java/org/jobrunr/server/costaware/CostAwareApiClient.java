package org.jobrunr.server.costaware;

import org.jobrunr.server.costaware.CostAwareScaleUpDto.InstanceEnvironment;
import org.jobrunr.server.costaware.CostAwareScaleUpDto.InstanceSpecifications;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.utils.mapper.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class CostAwareApiClient {

    private final HttpClient httpClient;
    private final CostAwareConfigurationReader costAwareConfigurationReader;
    private final JsonMapper jsonMapper;
    private final JobRunrMetadata clusterId;

    public CostAwareApiClient(CostAwareConfiguration costAwareConfiguration, JsonMapper jsonMapper, JobRunrMetadata clusterId) {
        this.httpClient = HttpClient.newHttpClient();
        this.costAwareConfigurationReader = new CostAwareConfigurationReader(costAwareConfiguration);
        this.jsonMapper = jsonMapper;
        this.clusterId = clusterId;
    }

    public void scaleUp() throws CostAwareApiClientException {
        try {
            Map<String, String> allEnvironmentVariables = costAwareConfigurationReader.getAdditionalEnvironmentVariables();
            allEnvironmentVariables.putAll(System.getenv());
            // TODO: Stop @class HashMap being appended to environment variables map

            CostAwareScaleUpDto scaleUpDto = new CostAwareScaleUpDto(
                    costAwareConfigurationReader.getProviderConfiguration().getProvider(),
                    costAwareConfigurationReader.getRegions(),
//                    clusterId.getId(),
                    // Cannot invoke "org.jobrunr.storage.JobRunrMetadata.getId()" because "this.clusterId" is null
                    "null",
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
}
