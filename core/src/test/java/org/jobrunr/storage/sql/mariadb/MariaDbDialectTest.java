package org.jobrunr.storage.sql.mariadb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MariaDbDialectTest {

    @Test
    void otherDBDoesNotSupportSelectForUpdateSkipLocked() {
        assertThat(new MariaDbDialect("MySQL", "10.6").selectForUpdateSkipLocked()).isEmpty();
    }

    @Test
    void mariaDB8DoesNotSupportSelectForUpdateSkipLocked() {
        assertThat(new MariaDbDialect("MariaDB", "8.0").selectForUpdateSkipLocked()).isEmpty();
    }

    @Test
    void mariaDB10DoesNotSupportSelectForUpdateSkipLocked() {
        assertThat(new MariaDbDialect("MariaDB", "10.5.0").selectForUpdateSkipLocked()).isEmpty();
    }

    @Test
    void mariaDB106DoesSupportSelectForUpdateSkipLocked() {
        assertThat(new MariaDbDialect("MariaDB", "10.6").selectForUpdateSkipLocked())
                .isEqualTo(" FOR UPDATE SKIP LOCKED");
    }
}