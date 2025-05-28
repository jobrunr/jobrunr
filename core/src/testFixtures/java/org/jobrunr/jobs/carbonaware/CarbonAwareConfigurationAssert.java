package org.jobrunr.jobs.carbonaware;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader;

import java.time.Duration;

public class CarbonAwareConfigurationAssert extends AbstractAssert<CarbonAwareConfigurationAssert, CarbonAwareJobProcessingConfigurationReader> {
    protected CarbonAwareConfigurationAssert(CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfigurationReader) {
        super(carbonAwareJobProcessingConfigurationReader, CarbonAwareConfigurationAssert.class);
    }

    protected CarbonAwareConfigurationAssert(CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration) {
        super(new CarbonAwareJobProcessingConfigurationReader(carbonAwareJobProcessingConfiguration), CarbonAwareConfigurationAssert.class);
    }

    public static CarbonAwareConfigurationAssert assertThat(CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfigurationReader) {
        return new CarbonAwareConfigurationAssert(carbonAwareJobProcessingConfigurationReader);
    }

    public static CarbonAwareConfigurationAssert assertThat(CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration) {
        return new CarbonAwareConfigurationAssert(carbonAwareJobProcessingConfiguration);
    }

    public CarbonAwareConfigurationAssert hasEnabled(boolean enabled) {
        Assertions.assertThat(actual.isEnabled()).isEqualTo(enabled);
        return this;
    }

    public CarbonAwareConfigurationAssert hasDataProvider(String dataProvider) {
        Assertions.assertThat(actual.getDataProvider()).isEqualTo(dataProvider);
        return this;
    }

    public CarbonAwareConfigurationAssert hasAreaCode(String areaCode) {
        Assertions.assertThat(actual.getAreaCode()).isEqualTo(areaCode);
        return this;
    }

    public CarbonAwareConfigurationAssert hasExternalCode(String externalCode) {
        Assertions.assertThat(actual.getExternalCode()).isEqualTo(externalCode);
        return this;
    }

    public CarbonAwareConfigurationAssert hasExternalIdentifier(String externalIdentifier) {
        Assertions.assertThat(actual.getExternalIdentifier()).isEqualTo(externalIdentifier);
        return this;
    }

    public CarbonAwareConfigurationAssert hasCarbonAwareApiUrl(String url) {
        Assertions.assertThat(actual.getCarbonIntensityApiUrl()).isEqualTo(url);
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
