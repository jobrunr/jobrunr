package org.jobrunr.server.tasks.steward;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.tasks.Task;

public abstract class AbstractJobStewardTask extends Task {

    protected AbstractJobStewardTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
    }

}
