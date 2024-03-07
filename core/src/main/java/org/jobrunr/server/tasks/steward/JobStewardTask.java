package org.jobrunr.server.tasks.steward;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.tasks.Task;

public abstract class JobStewardTask extends Task {

    protected JobStewardTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
    }

}
