package org.jobrunr.server.costaware;

import java.util.Map;

public record CostAwareScaleDownDto(
        String clusterId,
        Map<String, String> providerConfiguration
) {
}
