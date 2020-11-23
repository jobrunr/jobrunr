package org.jobrunr.storage.nosql.mongo;

import org.bson.BsonBinarySubType;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.types.Binary;

import java.util.UUID;

import static org.bson.internal.UuidHelper.decodeBinaryToUuid;
import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class MongoUtils {

    private MongoUtils() {

    }

    //sorry about this -> necessary to be compatible with MongoDB Java Driver 3 and 4
    //see https://github.com/jobrunr/jobrunr/issues/55
    static UUID getIdAsUUID(Document document) {
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

    public static byte[] clone(final byte[] array) {
        final byte[] result = new byte[array.length];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }
}
