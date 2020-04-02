package org.jobrunr.server;

public interface JobActivator {

    <T> T activateJob(Class<T> type);

}
