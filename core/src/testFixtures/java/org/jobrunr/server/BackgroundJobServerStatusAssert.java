package org.jobrunr.server;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.TemporalUnitOffset;
import org.jobrunr.storage.BackgroundJobServerStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static org.assertj.core.api.Assertions.within;

public class BackgroundJobServerStatusAssert extends AbstractAssert<BackgroundJobServerStatusAssert, BackgroundJobServerStatus> {
    protected BackgroundJobServerStatusAssert(BackgroundJobServerStatus backgroundJobServerStatus) {
        super(backgroundJobServerStatus, BackgroundJobServerStatusAssert.class);
    }

    public static BackgroundJobServerStatusAssert assertThat(BackgroundJobServerStatus backgroundJobServerStatus) {
        return new BackgroundJobServerStatusAssert(backgroundJobServerStatus);
    }

    public BackgroundJobServerStatusAssert isRepresentatifOf(BackgroundJobServerStatus serverStatus) {
        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("firstHeartbeat", "lastHeartbeat")
                // the systemCpuLoad/processCpuLoad columns are NUMERIC(3, 2)
                .withComparatorForType(BackgroundJobServerStatusAssert::compareRoundedCpuLoads, Double.class)
                .isEqualTo(serverStatus);
        hasFirstHeartbeatCloseTo(serverStatus.getFirstHeartbeat());
        hasLastHeartbeatCloseTo(serverStatus.getLastHeartbeat());
        return this;
    }

    public BackgroundJobServerStatusAssert hasFirstHeartbeatCloseTo(Instant firstHeartbeat) {
        return hasFirstHeartbeatCloseTo(firstHeartbeat, within(1000, ChronoUnit.MICROS));
    }

    public BackgroundJobServerStatusAssert hasFirstHeartbeatCloseTo(Instant firstHeartbeat, TemporalUnitOffset offset) {
        Assertions.assertThat(actual.getLastHeartbeat()).isCloseTo(firstHeartbeat, offset);
        return this;
    }

    public BackgroundJobServerStatusAssert hasLastHeartbeatCloseTo(Instant lastHeartbeat) {
        return hasLastHeartbeatCloseTo(lastHeartbeat, within(1000, ChronoUnit.MICROS));
    }

    public BackgroundJobServerStatusAssert hasLastHeartbeatCloseTo(Instant lastHeartbeat, TemporalUnitOffset offset) {
        Assertions.assertThat(actual.getLastHeartbeat()).isCloseTo(lastHeartbeat, offset);
        return this;
    }

    private static int compareRoundedCpuLoads(Double value1, Double value2) {
        if (value1 == null || value2 == null) {
            return Objects.equals(value1, value2) ? 0 : (value1 == null ? -1 : 1);
        }
        return toDatabaseScale(value1).compareTo(toDatabaseScale(value2));
    }

    private static BigDecimal toDatabaseScale(Double cpuLoad) {
        return BigDecimal.valueOf(cpuLoad).setScale(2, RoundingMode.HALF_UP);
    }
}
