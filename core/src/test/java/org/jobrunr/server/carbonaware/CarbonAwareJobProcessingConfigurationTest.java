package org.jobrunr.server.carbonaware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration.usingStandardCarbonAwareJobProcessingConfiguration;

class CarbonAwareJobProcessingConfigurationTest {
    CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration;

    @BeforeEach
    void setUp() {
        carbonAwareJobProcessingConfiguration = usingStandardCarbonAwareJobProcessingConfiguration();
    }

    @Test
    void andAreaCodeThrowsAnExceptionIfEitherExternalCodeOrExternalIdentifierIsSet() {
        assertThatCode(() -> carbonAwareJobProcessingConfiguration.andDataProvider("ENTSO-E").andExternalCode("IT-North").andAreaCode("IT-NO"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");

        assertThatCode(() -> carbonAwareJobProcessingConfiguration.andDataProvider("ENTSO-E").andExternalIdentifier("10Y1001A1001A73I").andAreaCode("IT-NO"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");
    }

    @Test
    void andExternalCodeThrowsAnExceptionIfDataProviderIsNotSet() {
        assertThatCode(() -> carbonAwareJobProcessingConfiguration.andExternalCode("IT-North"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Please set the dataProvider before setting the externalCode.");

        assertThatCode(() -> carbonAwareJobProcessingConfiguration.andDataProvider("ENTSO-E").andExternalCode("IT-North"))
                .doesNotThrowAnyException();
    }

    @Test
    void andExternalCodeThrowsAnExceptionIfEitherAreaCodeOrExternalIdentifierIsSet() {
        assertThatCode(() -> carbonAwareJobProcessingConfiguration.andAreaCode("IT-NO").andDataProvider("ENTSO-E").andExternalCode("IT-North"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");

        assertThatCode(() -> carbonAwareJobProcessingConfiguration.andDataProvider("ENTSO-E").andExternalIdentifier("10Y1001A1001A73I").andExternalCode("IT-North"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");
    }

    @Test
    void andExternalIdentifierThrowsAnExceptionIfDataProviderIsNotSet() {
        assertThatCode(() -> carbonAwareJobProcessingConfiguration.andExternalIdentifier("10Y1001A1001A73I"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Please set the dataProvider before setting the externalIdentifier.");

        assertThatCode(() -> carbonAwareJobProcessingConfiguration.andDataProvider("ENTSO-E").andExternalIdentifier("10Y1001A1001A73I"))
                .doesNotThrowAnyException();
    }

    @Test
    void andExternalIdentifierThrowsAnExceptionIfEitherAreaCodeOrExternalCodeIsSet() {
        assertThatCode(() -> carbonAwareJobProcessingConfiguration.andAreaCode("IT-NO").andDataProvider("ENTSO-E").andExternalIdentifier("10Y1001A1001A73I"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");

        assertThatCode(() -> carbonAwareJobProcessingConfiguration.andDataProvider("ENTSO-E").andExternalCode("IT-North").andExternalIdentifier("10Y1001A1001A73I"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");
    }

    @Test
    void testGetCarbonIntensityForecastQueryString() {
        CarbonAwareConfigurationReader reader = new CarbonAwareConfigurationReader(carbonAwareJobProcessingConfiguration);

        carbonAwareJobProcessingConfiguration.andAreaCode("IT-NO");
        assertThat(reader.getCarbonIntensityForecastQueryString()).isEqualTo("?region=IT-NO");

        carbonAwareJobProcessingConfiguration.andDataProvider("ENTSO-E");
        assertThat(reader.getCarbonIntensityForecastQueryString().substring(1).split("&"))
                .hasSize(2)
                .containsExactlyInAnyOrder("region=IT-NO", "dataProvider=ENTSO-E");

        carbonAwareJobProcessingConfiguration.andAreaCode(null).andExternalCode("IT-North");
        assertThat(reader.getCarbonIntensityForecastQueryString().substring(1).split("&"))
                .hasSize(2)
                .containsExactlyInAnyOrder("externalCode=IT-North", "dataProvider=ENTSO-E");

        carbonAwareJobProcessingConfiguration.andExternalCode(null).andExternalIdentifier("10Y1001A1001A73I");
        assertThat(reader.getCarbonIntensityForecastQueryString().substring(1).split("&"))
                .hasSize(2)
                .containsExactlyInAnyOrder("externalIdentifier=10Y1001A1001A73I", "dataProvider=ENTSO-E");
    }
}