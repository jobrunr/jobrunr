package org.jobrunr.utils.streams;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.streams.StreamUtils.batchCollector;

class StreamUtilsTest {

    @Test
    void canBatch() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> output = new ArrayList<>();

        int batchSize = 3;
        Consumer<List<Integer>> batchProcessor = xs -> {
            System.out.println("Adding " + xs.size());
            output.addAll(xs);
        };

        Long ignored = input.stream()
                .collect(batchCollector(batchSize, batchProcessor));

        assertThat(output).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }
}