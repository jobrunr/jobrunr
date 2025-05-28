package org.jobrunr.jobs.carbonaware;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader;

import java.time.Duration;

public class CarbonAwareJobProcessingConfigurationAssert extends AbstractAssert<CarbonAwareJobProcessingConfigurationAssert, CarbonAwareJobProcessingConfigurationReader> {
    protected CarbonAwareJobProcessingConfigurationAssert(CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfigurationReader) {
        super(carbonAwareJobProcessingConfigurationReader, CarbonAwareJobProcessingConfigurationAssert.class);
    }

    protected CarbonAwareJobProcessingConfigurationAssert(CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration) {
        super(new CarbonAwareJobProcessingConfigurationReader(carbonAwareJobProcessingConfiguration), CarbonAwareJobProcessingConfigurationAssert.class);
    }

    public static CarbonAwareJobProcessingConfigurationAssert assertThat(CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfigurationReader) {
        return new CarbonAwareJobProcessingConfigurationAssert(carbonAwareJobProcessingConfigurationReader);
    }

    public static CarbonAwareJobProcessingConfigurationAssert assertThat(CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration) {
        return new CarbonAwareJobProcessingConfigurationAssert(carbonAwareJobProcessingConfiguration);
    }

    public CarbonAwareJobProcessingConfigurationAssert hasEnabled(boolean enabled) {
        Assertions.assertThat(actual.isEnabled()).isEqualTo(enabled);
        return this;
    }

    public CarbonAwareJobProcessingConfigurationAssert hasDataProvider(String dataProvider) {
        Assertions.assertThat(actual.getDataProvider()).isEqualTo(dataProvider);
        return this;
    }

    public CarbonAwareJobProcessingConfigurationAssert hasAreaCode(String areaCode) {
        Assertions.assertThat(actual.getAreaCode()).isEqualTo(areaCode);
        return this;
    }

    public CarbonAwareJobProcessingConfigurationAssert hasExternalCode(String externalCode) {
        Assertions.assertThat(actual.getExternalCode()).isEqualTo(externalCode);
        return this;
    }

    public CarbonAwareJobProcessingConfigurationAssert hasExternalIdentifier(String externalIdentifier) {
        Assertions.assertThat(actual.getExternalIdentifier()).isEqualTo(externalIdentifier);
        return this;
    }

    public CarbonAwareJobProcessingConfigurationAssert hasCarbonAwareApiUrl(String url) {
        Assertions.assertThat(actual.getCarbonIntensityApiUrl()).isEqualTo(url);
        return this;
    }

    public CarbonAwareJobProcessingConfigurationAssert hasApiClientConnectTimeout(Duration connectTimeout) {
        Assertions.assertThat(actual.getApiClientConnectTimeout()).isEqualTo(connectTimeout);
        return this;
    }

    public CarbonAwareJobProcessingConfigurationAssert hasApiClientReadTimeout(Duration readTimeout) {
        Assertions.assertThat(actual.getApiClientReadTimeout()).isEqualTo(readTimeout);
        return this;
    }
}
