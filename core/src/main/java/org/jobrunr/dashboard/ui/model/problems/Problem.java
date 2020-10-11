package org.jobrunr.dashboard.ui.model.problems;

public abstract class Problem {

    public final String type;

    protected Problem(String type) {
        this.type = type;
    }
}
