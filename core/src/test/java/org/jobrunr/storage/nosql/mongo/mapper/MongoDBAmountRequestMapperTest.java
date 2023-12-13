package org.jobrunr.storage.nosql.mongo.mapper;

import org.jobrunr.storage.navigation.AmountRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

class MongoDBAmountRequestMapperTest {

    MongoDBAmountRequestMapper mapper = new MongoDBAmountRequestMapper();

    @Test
    void mapsAscOnUpdatedAtOrderCorrectly() {
        AmountRequest amountRequest = ascOnUpdatedAt(20);

        assertThat(mapper.mapToSort(amountRequest).toString()).isEqualTo("{\"updatedAt\": 1}");
    }

}