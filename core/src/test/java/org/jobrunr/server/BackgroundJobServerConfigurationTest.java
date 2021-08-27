package org.jobrunr.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

class BackgroundJobServerConfigurationTest {

    private BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();

    @Test
    void ifDefaultPollIntervalInSecondsSmallerThan5ThenThrowException() {
        assertThatThrownBy(() -> backgroundJobServerConfiguration.andPollIntervalInSeconds(4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The pollIntervalInSeconds can not be smaller than 5 - otherwise it will cause to much load on your SQL/noSQL datastore.");
    }

    @Test
    void ifDefaultPollIntervalInSeconds5OrHigherThenNoException() {
        assertThatCode(() -> backgroundJobServerConfiguration.andPollIntervalInSeconds(5)).doesNotThrowAnyException();
        assertThatCode(() -> backgroundJobServerConfiguration.andPollIntervalInSeconds(15)).doesNotThrowAnyException();
    }

}