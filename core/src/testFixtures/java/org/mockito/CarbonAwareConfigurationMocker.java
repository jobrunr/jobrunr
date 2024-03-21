package org.mockito;

import org.jobrunr.utils.carbonaware.CarbonAwareConfiguration;

public class CarbonAwareConfigurationMocker {

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
