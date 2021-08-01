package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.test.QuarkusUnitTest;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultConfigurationTest {

    @Inject
    StorageProvider storageProvider;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Test
    public void testDefaultDataSourceInjection() {
        assertThat(storageProvider).isInstanceOf(InMemoryStorageProvider.class);
    }
}
