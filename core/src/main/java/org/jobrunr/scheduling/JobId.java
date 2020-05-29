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
        return uuid.equals(obj);
    }

    @Override
    public String toString() {
        return uuid.toString();
    }
}
