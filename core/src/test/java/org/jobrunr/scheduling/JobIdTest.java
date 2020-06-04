package org.jobrunr.scheduling;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobIdTest {

    @Test
    void testEquals() {
        UUID uuid = UUID.randomUUID();
        final JobId jobId1 = new JobId(uuid);
        final JobId jobId2 = new JobId(uuid);

        assertThat(jobId1).isEqualTo(jobId2);
        assertThat(jobId1.hashCode()).isEqualTo(jobId2.hashCode());
    }

    @Test
    void testNotEquals() {
        final JobId jobId1 = new JobId(UUID.randomUUID());
        final JobId jobId2 = new JobId(UUID.randomUUID());

        assertThat(jobId1).isNotEqualTo(jobId2);
    }
}