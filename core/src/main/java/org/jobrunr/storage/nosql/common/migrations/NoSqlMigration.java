package org.jobrunr.storage.nosql.common.migrations;

import org.jobrunr.utils.reflection.ReflectionUtils;

import java.io.IOException;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public interface NoSqlMigration {

    String getClassName();

    Class<?> getMigrationClass() throws IOException, ClassNotFoundException;

    default <T> T getMigration() throws IOException, ClassNotFoundException {
        return cast(ReflectionUtils.newInstance(getMigrationClass()));
    }
}
