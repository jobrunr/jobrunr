package org.jobrunr.dashboard.ui.model.problems;

import java.time.Instant;

public class CarbonIntensityApiErrorProblem extends Problem {

    public static final String PROBLEM_TYPE = "carbon-intensity-api-error";

    protected CarbonIntensityApiErrorProblem() {
        super(PROBLEM_TYPE);
    }

    protected CarbonIntensityApiErrorProblem(Instant createdAt) {
        super(PROBLEM_TYPE, createdAt);
    }

}
