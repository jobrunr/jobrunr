package org.jobrunr.storage.nosql.couchbase;

import com.couchbase.client.java.json.JsonObject;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MICROS;

public class CouchbaseUtils {

    private CouchbaseUtils() {

    }

    public static UUID getIdAsUUID(JsonObject document) {
        if (document.get("_id") instanceof UUID) {
            return (UUID) document.get("_id");
        }
        return UUID.fromString(document.get("_id").toString());
    }

    public static long toMicroSeconds(Instant instant) {
        return MICROS.between(Instant.EPOCH, instant);
    }

    public static Instant fromMicroseconds(Long micros) {
        if (micros == null) {
            return null;
        }
        return Instant.EPOCH.plus(Duration.of(micros, ChronoUnit.MICROS));
    }

    public static String toCouchbaseId(String id) {
        return "_" + id;
    }

}
