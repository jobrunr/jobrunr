package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;
import org.jobrunr.storage.sql.common.db.dialect.Dialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.storage.StorageProviderUtils.Metadata.*;

public class MetadataTable extends Sql<JobRunrMetadata> {

    public MetadataTable(Connection connection, Dialect dialect, String tablePrefix) {
        this
                .using(connection, dialect, tablePrefix, "jobrunr_metadata")
                .with(FIELD_ID, JobRunrMetadata::getId)
                .with(FIELD_NAME, JobRunrMetadata::getName)
                .with(FIELD_OWNER, JobRunrMetadata::getOwner)
                .with(FIELD_VALUE, JobRunrMetadata::getValue)
                .with(FIELD_CREATED_AT, JobRunrMetadata::getCreatedAt)
                .with(FIELD_UPDATED_AT, metadata -> Instant.now());
    }

    public MetadataTable withId(String id) {
        with(FIELD_ID, id);
        return this;
    }

    public JobRunrMetadata save(JobRunrMetadata metadata) throws SQLException {
        withId(metadata.getId());

        if (selectExists("from jobrunr_metadata where id = :id")) {
            update(metadata, "jobrunr_metadata SET value = :value, updatedAt = :updatedAt WHERE id = :id");
        } else {
            insert(metadata, "into jobrunr_metadata values(:id, :name, :owner, :value, :createdAt, :updatedAt)");
        }
        return metadata;
    }

    public JobRunrMetadata get(String name, String owner) {
        return with(FIELD_NAME, name)
                .with(FIELD_OWNER, owner)
                .select("* from jobrunr_metadata where name = :name and owner = :owner")
                .map(this::toJobRunrMetadata)
                .findFirst()
                .orElse(null);
    }

    public List<JobRunrMetadata> getAll(String name) {
        return with(FIELD_NAME, name)
                .withOrderLimitAndOffset("updatedAt ASC", 1000, 0)
                .select("* from jobrunr_metadata where name = :name")
                .map(this::toJobRunrMetadata)
                .collect(toList());
    }

    public void incrementCounter(String id, int amount) throws SQLException {
        this
                .with(FIELD_ID, id)
                .with("amount", amount)
                .update("jobrunr_metadata set value = cast((cast(cast(value as char(10)) as decimal) + :amount) as char(10)) where id = :id");
    }

    public int deleteByName(String name) throws SQLException {
        return with(FIELD_NAME, name)
                .delete("from jobrunr_metadata where name = :name");
    }

    private JobRunrMetadata toJobRunrMetadata(SqlResultSet resultSet) {
        return new JobRunrMetadata(
                resultSet.asString(FIELD_NAME),
                resultSet.asString(FIELD_OWNER),
                resultSet.asString(FIELD_VALUE),
                resultSet.asInstant(FIELD_CREATED_AT),
                resultSet.asInstant(FIELD_UPDATED_AT)
        );
    }
}
