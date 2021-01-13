package org.jobrunr.storage.sql.common.migrations;

import java.io.IOException;

public interface Migration {

    String getFileName();

    String getMigrationSql() throws IOException;
}
