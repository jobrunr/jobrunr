package org.jobrunr.storage;

public class ServerTimedOutException extends StorageException {

    public ServerTimedOutException(BackgroundJobServerStatus serverStatus, StorageException e) {
        super("Server " + serverStatus.getId() + " has timed out and must reannounce itself", e);
    }

}
