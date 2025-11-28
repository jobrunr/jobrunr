package org.jobrunr.utils.streams;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BatchCollectorTest {

    List<UUID> uuids = new ArrayList<>();

    @Test
    void batchCollectorReturnsLongAndAllItemsProcessedA() {
        Stream<UUID> employeeStream = IntStream.range(0, 10000)
                .mapToObj(obj -> UUID.randomUUID());

        Long totalAmount = employeeStream.collect(StreamUtils.batchCollector(5000, this::batchProcessor));

        assertThat(uuids).hasSize(10000);
        assertThat(totalAmount).isEqualTo(10000);
    }

    @Test
    void batchCollectorReturnsLongAndAllItemsProcessedB() {
        Stream<UUID> employeeStream = IntStream.range(0, 123762)
                .mapToObj(obj -> UUID.randomUUID());

        Long totalAmount = employeeStream.collect(StreamUtils.batchCollector(5000, this::batchProcessor));

        assertThat(uuids).hasSize(123762);
        assertThat(totalAmount).isEqualTo(123762);
    }

    private List<UUID> batchProcessor(List<UUID> idsToBatch) {
        uuids.addAll(idsToBatch);
        return idsToBatch;
    }
}