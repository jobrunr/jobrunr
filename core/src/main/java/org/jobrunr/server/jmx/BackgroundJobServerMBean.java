package org.jobrunr.server.jmx;

import org.jobrunr.storage.BackgroundJobServerStatus;

import java.util.UUID;

public interface BackgroundJobServerMBean {

    UUID getId();

    BackgroundJobServerStatus getServerStatus();

    boolean isRunning();

    void start();

    void pauseProcessing();

    void resumeProcessing();

    void stop();

}
