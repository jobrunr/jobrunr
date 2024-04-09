package org.jobrunr.storage.sql.mysql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlDialectTest {

    @Test
    void otherDBDoesNotSupportSelectForUpdateSkipLocked() {
        assertThat(new MySqlDialect("MariaDB", "10.6").selectForUpdateSkipLocked()).isEmpty();
    }

    @Test
    void mySQL5DoesNotSupportSelectForUpdateSkipLocked() {
        assertThat(new MySqlDialect("MySQL", "5.8").selectForUpdateSkipLocked()).isEmpty();
    }

    @Test
    void mySQL8DoesNotSupportSelectForUpdateSkipLocked() {
        assertThat(new MySqlDialect("MySQL", "8.0.0").selectForUpdateSkipLocked()).isEmpty();
    }

    @Test
    void mySQL801DoesSupportSelectForUpdateSkipLocked() {
        assertThat(new MySqlDialect("MySQL", "8.0.1").selectForUpdateSkipLocked())
                .isEqualTo(" FOR UPDATE SKIP LOCKED");
    }

    @Test
    void mySQL830DoesSupportSelectForUpdateSkipLocked() {
        assertThat(new MySqlDialect("MySQL", "8.3.0").selectForUpdateSkipLocked())
                .isEqualTo(" FOR UPDATE SKIP LOCKED");
    }
}