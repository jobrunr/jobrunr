package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.test.junit.QuarkusTest;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class DefaultConfigurationTest {

    @Inject
    StorageProvider storageProvider;

    @Test
    public void testDefaultDataSourceInjection() {
        assertThat(storageProvider).isInstanceOf(InMemoryStorageProvider.class);
    }
}
