package org.jobrunr.dashboard.ui.model.problems;

import java.util.ArrayList;

public class Problems extends ArrayList<Problem> {

    public void addProblem(Problem problem) {
        this.add(problem);
    }

    public void removeProblemsOfType(String type) {
        removeIf(problem -> type.equals(problem.type));
    }
}
