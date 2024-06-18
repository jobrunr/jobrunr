package org.jobrunr.utils.carbonaware;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.server.carbonaware.CarbonAwareConfiguration;
import org.jobrunr.server.carbonaware.CarbonAwareConfigurationReader;

import java.time.Duration;

public class CarbonAwareConfigurationAssert extends AbstractAssert<CarbonAwareConfigurationAssert, CarbonAwareConfigurationReader> {
    protected CarbonAwareConfigurationAssert(CarbonAwareConfigurationReader carbonAwareConfigurationReader) {
        super(carbonAwareConfigurationReader, CarbonAwareConfigurationAssert.class);
    }

    protected CarbonAwareConfigurationAssert(CarbonAwareConfiguration carbonAwareConfiguration) {
        super(new CarbonAwareConfigurationReader(carbonAwareConfiguration), CarbonAwareConfigurationAssert.class);
    }

    public static CarbonAwareConfigurationAssert assertThat(CarbonAwareConfigurationReader carbonAwareConfigurationReader) {
        return new CarbonAwareConfigurationAssert(carbonAwareConfigurationReader);
    }

    public static CarbonAwareConfigurationAssert assertThat(CarbonAwareConfiguration carbonAwareConfiguration) {
        return new CarbonAwareConfigurationAssert(carbonAwareConfiguration);
    }

    public CarbonAwareConfigurationAssert hasAreaCode(String areaCode) {
        Assertions.assertThat(actual.getAreaCode()).isEqualTo(areaCode);
        return this;
    }

    public CarbonAwareConfigurationAssert hasCarbonAwareApiUrl(String url) {
        Assertions.assertThat(actual.getCarbonAwareApiUrl()).isEqualTo(url);
        return this;
    }

    public CarbonAwareConfigurationAssert hasApiClientConnectTimeout(Duration connectTimeout) {
        Assertions.assertThat(actual.getApiClientConnectTimeout()).isEqualTo(connectTimeout);
        return this;
    }

    public CarbonAwareConfigurationAssert hasApiClientReadTimeout(Duration readTimeout) {
        Assertions.assertThat(actual.getApiClientReadTimeout()).isEqualTo(readTimeout);
        return this;
    }
}
