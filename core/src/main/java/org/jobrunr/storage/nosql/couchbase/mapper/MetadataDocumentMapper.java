package org.jobrunr.storage.nosql.couchbase.mapper;

import com.couchbase.client.java.json.JsonObject;
import org.jobrunr.storage.JobRunrMetadata;

import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.fromMicroseconds;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.toCouchbaseId;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.toMicroSeconds;

public class MetadataDocumentMapper {

    public JsonObject toUpdateDocument(JobRunrMetadata metadata) {
        final JsonObject document = JsonObject.create();
        document.put(toCouchbaseId(Metadata.FIELD_ID), metadata.getId());
        document.put(Metadata.FIELD_NAME, metadata.getName());
        document.put(Metadata.FIELD_OWNER, metadata.getOwner());
        document.put(Metadata.FIELD_VALUE, metadata.getValue());
        document.put(Metadata.FIELD_CREATED_AT, toMicroSeconds(metadata.getCreatedAt()));
        document.put(Metadata.FIELD_UPDATED_AT, toMicroSeconds(metadata.getUpdatedAt()));

        return document;
    }

    public JobRunrMetadata toJobRunrMetadata(JsonObject document) {
        if (document == null) return null;

        return new JobRunrMetadata(
                document.getString(Metadata.FIELD_NAME),
                document.getString(Metadata.FIELD_OWNER),
                document.getString(Metadata.FIELD_VALUE),
                fromMicroseconds(document.getLong(Metadata.FIELD_CREATED_AT)),
                fromMicroseconds(document.getLong(Metadata.FIELD_UPDATED_AT))
        );
    }
}
