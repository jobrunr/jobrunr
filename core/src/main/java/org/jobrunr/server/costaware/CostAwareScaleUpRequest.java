package org.jobrunr.server.costaware;

import java.util.Map;

public record CostAwareScaleUpRequest(
        String provider,
        String[] regions,
        String clusterId,
        Map<String, String> providerConfiguration,
        InstanceSpecifications instanceSpecifications,
        InstanceEnvironment instanceEnvironment
) {

    // TODO InstanceSpecification (drop plural?)
    public record InstanceSpecifications(
            Integer minVcpuCores,
            Double minMemoryGiB,
            Double minGpuMemoryGiB
    ) {}

    public record InstanceEnvironment(
            String dockerImage,
            String imageArchitecture,
            Map<String, String> environmentVariables
    ) {}
}
