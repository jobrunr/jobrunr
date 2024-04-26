package org.jobrunr.utils.carbonaware;

import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;


public class CarbonAwareBackgroundJobByJobRequestTest extends AbstractCarbonAwareWiremockTest {

    StorageProvider storageProvider;

    @BeforeEach
    void setUpStorageProvider() {
        storageProvider = new InMemoryStorageProvider();
    }

    //TODO: add tests

}
