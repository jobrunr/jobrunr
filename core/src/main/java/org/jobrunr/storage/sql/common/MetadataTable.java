package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MetadataTable extends Sql<JobRunrMetadata> {

    public MetadataTable(DataSource dataSource) {
        this
                .using(dataSource)
                .with("id", JobRunrMetadata::getId)
                .with("name", JobRunrMetadata::getName)
                .with("owner", JobRunrMetadata::getOwner)
                .with("value", JobRunrMetadata::getValue)
                .with("createdAt", JobRunrMetadata::getCreatedAt)
                .with("updatedAt", metadata -> Instant.now());
    }

    public MetadataTable withId(String id) {
        with("id", id);
        return this;
    }

    public JobRunrMetadata save(JobRunrMetadata metadata) {
        withId(metadata.getId());

        if (selectExists("from jobrunr_metadata where id = :id")) {
            update(metadata, "jobrunr_metadata SET value = :value, updatedAt = :updatedAt WHERE id = :id");
        } else {
            insert(metadata, "into jobrunr_metadata values(:id, :name, :owner, :value, :createdAt, :updatedAt)");
        }
        return metadata;
    }

    public JobRunrMetadata get(String name, String owner) {
        return with("name", name)
                .with("owner", owner)
                .select("* from jobrunr_metadata where name = :name and owner = :owner")
                .map(this::toJobRunrMetadata)
                .findFirst()
                .orElse(null);
    }

    public List<JobRunrMetadata> getAll(String name) {
        return with("name", name)
                .withOrderLimitAndOffset("updatedAt ASC", 1000, 0)
                .select("* from jobrunr_metadata where name = :name")
                .map(this::toJobRunrMetadata)
                .collect(toList());
    }

    public int deleteByKey(String name) {
        return with("name", name)
                .delete("from jobrunr_metadata where name = :name");
    }

    private JobRunrMetadata toJobRunrMetadata(SqlResultSet resultSet) {
        return new JobRunrMetadata(
                resultSet.asString("name"),
                resultSet.asString("owner"),
                resultSet.asString("value"),
                resultSet.asInstant("createdAt"),
                resultSet.asInstant("updatedAt")
        );
    }
}
