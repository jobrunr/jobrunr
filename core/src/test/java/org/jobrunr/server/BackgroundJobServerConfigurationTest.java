package org.jobrunr.server;

import org.junit.jupiter.api.Test;

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
    void isServerTimeoutMultiplicandIsSmallerThan4AnExceptionIsThrown() {
        assertThatThrownBy(() -> backgroundJobServerConfiguration.andServerTimeoutPollIntervalMultiplicand(3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The smallest possible ServerTimeoutPollIntervalMultiplicand is 4 (4 is also the default)");
    }

}