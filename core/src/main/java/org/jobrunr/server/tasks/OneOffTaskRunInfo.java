package org.jobrunr.server.tasks;

import org.jobrunr.server.BackgroundJobServerConfigurationReader;

public class OneOffTaskRunInfo extends TaskRunInfo {

    public OneOffTaskRunInfo(BackgroundJobServerConfigurationReader backgroundJobServerConfiguration) {
        super(backgroundJobServerConfiguration);
    }
}