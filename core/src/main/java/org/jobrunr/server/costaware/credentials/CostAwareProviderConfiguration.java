package org.jobrunr.server.costaware.credentials;

import java.util.Map;

public interface CostAwareProviderConfiguration {
    Map<String, String> asMap();

    String getProvider();
}
