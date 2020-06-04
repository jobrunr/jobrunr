package org.jobrunr.scheduling;

import java.util.UUID;

/**
 * Class which represents the Id of the job.
 */
public class JobId {

    private final UUID uuid;

    public JobId(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID asUUID() {
        return uuid;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JobId) {
            return uuid.equals(((JobId) obj).uuid);
        }
        return false;
    }

    @Override
    public String toString() {
        return uuid.toString();
    }

    public static JobId parse(String uuidAsString) {
        return new JobId(UUID.fromString(uuidAsString));
    }
}
