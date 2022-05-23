package org.jobrunr.storage.nosql.mongo.mapper;

import org.bson.Document;
import org.jobrunr.storage.JobRunrMetadata;

import java.util.Date;

import static org.jobrunr.storage.StorageProviderUtils.Metadata;

public class MetadataDocumentMapper {

    public Document toInsertDocument(JobRunrMetadata metadata) {
        final Document document = new Document();
        document.put("_id", metadata.getId());
        document.put(Metadata.FIELD_NAME, metadata.getName());
        document.put(Metadata.FIELD_OWNER, metadata.getOwner());
        document.put(Metadata.FIELD_VALUE, metadata.getValue());
        return document;
    }

    public Document toUpdateDocument(JobRunrMetadata metadata) {
        final Document document = new Document();
        document.put("_id", metadata.getId());
        document.put(Metadata.FIELD_NAME, metadata.getName());
        document.put(Metadata.FIELD_OWNER, metadata.getOwner());
        document.put(Metadata.FIELD_VALUE, metadata.getValue());
        document.put(Metadata.FIELD_CREATED_AT, metadata.getCreatedAt());
        document.put(Metadata.FIELD_UPDATED_AT, metadata.getUpdatedAt());

        return new Document("$set", document);
    }

    public JobRunrMetadata toJobRunrMetadata(Document document) {
        if(document == null) return null;

        return new JobRunrMetadata(
                document.getString(Metadata.FIELD_NAME),
                document.getString(Metadata.FIELD_OWNER),
                document.getString(Metadata.FIELD_VALUE),
                document.get(Metadata.FIELD_CREATED_AT, Date.class).toInstant(),
                document.get(Metadata.FIELD_UPDATED_AT, Date.class).toInstant()
        );
    }
}
