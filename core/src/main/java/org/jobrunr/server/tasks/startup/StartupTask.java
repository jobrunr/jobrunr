package org.jobrunr.server.tasks.startup;

import java.util.Arrays;
import java.util.List;

public class StartupTask implements Runnable {

    private final List<Runnable> tasks;

    public StartupTask(Runnable... tasks) {
        this.tasks = Arrays.asList(tasks);
    }

    @Override
    public void run() {
        tasks.forEach(Runnable::run);
    }
}
