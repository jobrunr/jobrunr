package org.jobrunr.storage.nosql.mongo;

import org.bson.BsonBinarySubType;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.types.Binary;

import java.time.Instant;
import java.util.UUID;

import static org.bson.internal.UuidHelper.decodeBinaryToUuid;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_CREATED_AT;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_UPDATED_AT;
import static org.jobrunr.utils.CollectionUtils.asSet;
import static org.jobrunr.utils.TimeUtils.toMicroSeconds;

public class MongoUtils {

    private MongoUtils() {

    }

    //sorry about this -> necessary to be compatible with MongoDB Java Driver 3 and 4
    //see https://github.com/jobrunr/jobrunr/issues/55
    public static UUID getIdAsUUID(Document document) {
        if (document.get("_id") instanceof UUID) {
            return document.get("_id", UUID.class);
        }

        Binary idAsBinary = document.get("_id", Binary.class);
        if (BsonBinarySubType.isUuid(idAsBinary.getType())) {
            if (idAsBinary.getType() == BsonBinarySubType.UUID_STANDARD.getValue()) {
                return decodeBinaryToUuid(clone(idAsBinary.getData()), idAsBinary.getType(), UuidRepresentation.STANDARD);
            } else if (idAsBinary.getType() == BsonBinarySubType.UUID_LEGACY.getValue()) {
                return decodeBinaryToUuid(clone(idAsBinary.getData()), idAsBinary.getType(), UuidRepresentation.JAVA_LEGACY);
            }
        }

        throw shouldNotHappenException("Unknown id: " + document.get("_id").getClass());
    }

    public static Object toValueForBson(String fieldName, String value) {
        if (asSet(FIELD_CREATED_AT, FIELD_UPDATED_AT, FIELD_SCHEDULED_AT).contains(fieldName)) {
            return toMicroSeconds(Instant.parse(value));
        }
        return value;
    }

    public static byte[] clone(final byte[] array) {
        final byte[] result = new byte[array.length];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }

}
