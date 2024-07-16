package org.jobrunr.server.carbonaware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.server.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;

class CarbonAwareConfigurationTest {
    CarbonAwareConfiguration carbonAwareConfiguration;

    @BeforeEach
    void setUp() {
        carbonAwareConfiguration = usingStandardCarbonAwareConfiguration();
    }

    @Test
    void andAreaCodeThrowsAnExceptionIfEitherExternalCodeOrExternalIdentifierIsSet() {
        assertThatCode(() -> carbonAwareConfiguration.andDataProvider("ENTSO-E").andExternalCode("IT-North").andAreaCode("IT-NO"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");

        assertThatCode(() -> carbonAwareConfiguration.andDataProvider("ENTSO-E").andExternalIdentifier("10Y1001A1001A73I").andAreaCode("IT-NO"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");
    }

    @Test
    void andExternalCodeThrowsAnExceptionIfDataProviderIsNotSet() {
        assertThatCode(() -> carbonAwareConfiguration.andExternalCode("IT-North"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Please set the dataProvider must be setting the externalCode.");

        assertThatCode(() -> carbonAwareConfiguration.andDataProvider("ENTSO-E").andExternalCode("IT-North"))
                .doesNotThrowAnyException();
    }

    @Test
    void andExternalCodeThrowsAnExceptionIfEitherAreaCodeOrExternalIdentifierIsSet() {
        assertThatCode(() -> carbonAwareConfiguration.andAreaCode("IT-NO").andDataProvider("ENTSO-E").andExternalCode("IT-North"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");

        assertThatCode(() -> carbonAwareConfiguration.andDataProvider("ENTSO-E").andExternalIdentifier("10Y1001A1001A73I").andExternalCode("IT-North"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");
    }

    @Test
    void andExternalIdentifierThrowsAnExceptionIfDataProviderIsNotSet() {
        assertThatCode(() -> carbonAwareConfiguration.andExternalIdentifier("10Y1001A1001A73I"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Please set the dataProvider must be setting the externalIdentifier.");

        assertThatCode(() -> carbonAwareConfiguration.andDataProvider("ENTSO-E").andExternalIdentifier("10Y1001A1001A73I"))
                .doesNotThrowAnyException();
    }

    @Test
    void andExternalIdentifierThrowsAnExceptionIfEitherAreaCodeOrExternalCodeIsSet() {
        assertThatCode(() -> carbonAwareConfiguration.andAreaCode("IT-NO").andDataProvider("ENTSO-E").andExternalIdentifier("10Y1001A1001A73I"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");

        assertThatCode(() -> carbonAwareConfiguration.andDataProvider("ENTSO-E").andExternalCode("IT-North").andExternalIdentifier("10Y1001A1001A73I"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only set either areaCode, externalCode or externalIdentifier.");
    }

    @Test
    void testGetCarbonIntensityForecastQueryString() {
        CarbonAwareConfigurationReader reader = new CarbonAwareConfigurationReader(carbonAwareConfiguration);

        carbonAwareConfiguration.andAreaCode("IT-NO");
        assertThat(reader.getCarbonIntensityForecastQueryString()).isEqualTo("?region=IT-NO");

        carbonAwareConfiguration.andDataProvider("ENTSO-E");
        assertThat(reader.getCarbonIntensityForecastQueryString().substring(1).split("&"))
                .hasSize(2)
                .containsExactlyInAnyOrder("region=IT-NO", "dataProvider=ENTSO-E");

        carbonAwareConfiguration.andAreaCode(null).andExternalCode("IT-North");
        assertThat(reader.getCarbonIntensityForecastQueryString().substring(1).split("&"))
                .hasSize(2)
                .containsExactlyInAnyOrder("externalCode=IT-North", "dataProvider=ENTSO-E");

        carbonAwareConfiguration.andExternalCode(null).andExternalIdentifier("10Y1001A1001A73I");
        assertThat(reader.getCarbonIntensityForecastQueryString().substring(1).split("&"))
                .hasSize(2)
                .containsExactlyInAnyOrder("externalIdentifier=10Y1001A1001A73I", "dataProvider=ENTSO-E");
    }
}