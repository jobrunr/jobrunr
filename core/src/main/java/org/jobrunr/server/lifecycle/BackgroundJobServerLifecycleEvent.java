package org.jobrunr.server.lifecycle;

public enum BackgroundJobServerLifecycleEvent {
    START(true),
    PAUSE(false),
    RESUME(true),
    STOP(false);

    final boolean isRunning;

    BackgroundJobServerLifecycleEvent(boolean isRunning) {
        this.isRunning = isRunning;
    }
}
