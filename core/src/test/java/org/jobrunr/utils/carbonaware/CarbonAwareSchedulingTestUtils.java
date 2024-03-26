package org.jobrunr.utils.carbonaware;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class CarbonAwareSchedulingTestUtils {
    public static void mockResponseWhenRequestingArea(String area, String response, WireMockServer wireMockServer) {
        String url = String.format("/carbon-intensity/v1/day-ahead-energy-prices?area=%s", area);
        wireMockServer.stubFor(WireMock.get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    public static MockedStatic<CarbonAwareConfiguration> mockCarbonAwareConf(String area) {
        MockedStatic<CarbonAwareConfiguration> carbonAwareConfigurationMock = Mockito.mockStatic(CarbonAwareConfiguration.class, Mockito.CALLS_REAL_METHODS);
        carbonAwareConfigurationMock.when(CarbonAwareConfiguration::isEnabled).thenReturn(true);
        carbonAwareConfigurationMock.when(CarbonAwareConfiguration::getArea).thenReturn(area);
        carbonAwareConfigurationMock.when(CarbonAwareConfiguration::getState).thenReturn(null);
        carbonAwareConfigurationMock.when(CarbonAwareConfiguration::getCloudProvider).thenReturn(null);
        carbonAwareConfigurationMock.when(CarbonAwareConfiguration::getCloudRegion).thenReturn(null);
        carbonAwareConfigurationMock.when(CarbonAwareConfiguration::getCarbonAwareApiBaseUrl).thenReturn("http://localhost:10000/carbon-intensity");
        return carbonAwareConfigurationMock;
    }
}
