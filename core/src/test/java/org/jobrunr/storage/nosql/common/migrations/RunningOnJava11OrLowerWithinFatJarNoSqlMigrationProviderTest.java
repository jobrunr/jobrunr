package org.jobrunr.storage.nosql.common.migrations;

import org.assertj.core.api.Assertions;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.zip.ZipEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunningOnJava11OrLowerWithinFatJarNoSqlMigrationProviderTest {

    @Mock
    ZipEntry zipEntry;

    RunningOnJava11OrLowerWithinFatJarNoSqlMigrationProvider noSqlMigrationProvider = new RunningOnJava11OrLowerWithinFatJarNoSqlMigrationProvider();

    @Test
    void isNoSqlMigrationReturnsFalseIfInputIsNotAClass() {
        when(zipEntry.getName()).thenReturn("org/jobrunr/storage/nosql/mongo/migrations/");

        assertThat(noSqlMigrationProvider.isNoSqlMigration(MongoDBStorageProvider.class, zipEntry)).isFalse();
    }

    @Test
    void isNoSqlMigrationReturnsTrueIsInputIsValidMigrationClas() {
        when(zipEntry.getName()).thenReturn("org/jobrunr/storage/nosql/mongo/migrations/M001_CreateJobCollection.class");

        assertThat(noSqlMigrationProvider.isNoSqlMigration(MongoDBStorageProvider.class, zipEntry)).isTrue();
    }

}