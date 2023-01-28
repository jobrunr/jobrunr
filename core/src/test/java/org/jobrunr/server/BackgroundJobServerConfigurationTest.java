package org.jobrunr.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

class BackgroundJobServerConfigurationTest {

    private BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();

    @Test
    void ifNameIsNullThenExceptionIsThrown() {
        assertThatThrownBy(() -> backgroundJobServerConfiguration.andName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The name can not be null or empty");
    }

    @Test
    void ifNameIsEmptyThenExceptionIsThrown() {
        assertThatThrownBy(() -> backgroundJobServerConfiguration.andName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The name can not be null or empty");
    }

    @Test
    void ifNameIsLongerThan128CharactersThenExceptionIsThrown() {
        assertThatThrownBy(() -> backgroundJobServerConfiguration.andName(String.format("%0" + 128 + "d", 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The length of the name can not exceed 128 characters");
    }

    @Test
    void ifDefaultPollIntervalInSecondsSmallerThan5ThenExceptionIsThrown() {
        assertThatThrownBy(() -> backgroundJobServerConfiguration.andPollIntervalInSeconds(4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The pollIntervalInSeconds can not be smaller than 5 - otherwise it will cause to much load on your SQL/noSQL datastore.");
    }

    @Test
    void ifDefaultPollIntervalInSeconds5OrHigherThenNoExceptionIsThrown() {
        assertThatCode(() -> backgroundJobServerConfiguration.andPollIntervalInSeconds(5)).doesNotThrowAnyException();
        assertThatCode(() -> backgroundJobServerConfiguration.andPollIntervalInSeconds(15)).doesNotThrowAnyException();
    }

}