package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.sql.common.db.Dialect;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;
import org.jobrunr.storage.sql.common.mapper.SqlAmountRequestMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;
import static org.jobrunr.storage.StorageProviderUtils.Metadata.*;
import static org.jobrunr.utils.CollectionUtils.asSet;

public class MetadataTable extends Sql<JobRunrMetadata> {

    private final SqlAmountRequestMapper amountRequestMapper;

    public MetadataTable(Connection connection, Dialect dialect, String tablePrefix) {
        this.amountRequestMapper = new SqlAmountRequestMapper(dialect, asSet(FIELD_NAME, FIELD_CREATED_AT, FIELD_UPDATED_AT));
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

    public MetadataTable withName(String name) {
        with(FIELD_NAME, name);
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
        return withName(name)
                .with(FIELD_OWNER, owner)
                .select("* from jobrunr_metadata where name = :name and owner = :owner")
                .map(this::toJobRunrMetadata)
                .findFirst()
                .orElse(null);
    }

    public List<JobRunrMetadata> getAll(String name) {
        return withName(name)
                .select("* from jobrunr_metadata where name = :name", ascOnUpdatedAt(1000))
                .map(this::toJobRunrMetadata)
                .collect(toList());
    }

    public void incrementCounter(String id, int amount) throws SQLException {
        this
                .with(FIELD_ID, id)
                .with("amount", amount)
                .update("jobrunr_metadata set value = cast(round((cast(cast( value as char(10) ) as decimal(10, 0)) + :amount), 0) as char(10)) where id = :id");
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

    private Stream<SqlResultSet> select(String statement, AmountRequest amountRequest) {
        return super.select(statement, amountRequestMapper.mapToSqlQuery(amountRequest, this));
    }
}
