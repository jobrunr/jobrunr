package org.jobrunr.server.costaware;

import java.util.Map;

public record CostAwareScaleDownRequest(
        String clusterId,
        Map<String, String> providerConfiguration
) {
}
