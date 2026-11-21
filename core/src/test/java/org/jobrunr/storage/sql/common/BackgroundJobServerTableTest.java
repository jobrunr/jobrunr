package org.jobrunr.storage.sql.common;

import org.h2.jdbcx.JdbcDataSource;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BackgroundJobServerTableTest {

    @Test
    void heartbeatStoresProcessCpuLoadInTheProcessCpuLoadColumn() {
        H2StorageProvider storageProvider = newStorageProvider();
        UUID serverId = UUID.randomUUID();

        storageProvider.announceBackgroundJobServer(serverStatus(serverId));
        storageProvider.signalBackgroundJobServerAlive(serverStatus(serverId));

        BackgroundJobServerStatus stored = storageProvider.getBackgroundJobServers().get(0);
        assertThat(stored.getSystemCpuLoad()).isEqualTo(0.90);
        assertThat(stored.getProcessCpuLoad()).isEqualTo(0.10);
    }

    private static BackgroundJobServerStatus serverStatus(UUID id) {
        Instant now = Instant.now();
        return new BackgroundJobServerStatus(id, "test-server", 4, 15, Duration.ofHours(36), Duration.ofHours(72),
                now, now, true,
                8_000_000_000L, 4_000_000_000L, 0.90,
                2_000_000_000L, 1_000_000_000L, 500_000_000L, 0.10);
    }

    private static H2StorageProvider newStorageProvider() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test-process-cpu-load-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        H2StorageProvider storageProvider = new H2StorageProvider(dataSource);
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return storageProvider;
    }
}
