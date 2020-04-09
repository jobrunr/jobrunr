package org.jobrunr.server;

@FunctionalInterface
public interface JobActivator {

    <T> T activateJob(Class<T> type);

}
