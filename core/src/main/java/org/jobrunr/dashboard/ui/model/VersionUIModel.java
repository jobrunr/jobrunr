package org.jobrunr.dashboard.ui.model;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.utils.metadata.VersionRetriever;

public class VersionUIModel {

    private String version;

    public VersionUIModel() {
        this.version = VersionRetriever.getVersion(JobRunr.class);
    }

    public String getVersion() {
        return version;
    }
}
