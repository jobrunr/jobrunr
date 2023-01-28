package org.jobrunr.dashboard.ui.model.problems;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Problems extends ConcurrentLinkedQueue<Problem> {

    public void addProblem(Problem problem) {
        removeIf(someProblem -> someProblem.type.equals(problem.type));
        this.add(problem);
    }

    public void removeProblemsOfType(String type) {
        removeIf(problem -> type.equals(problem.type));
    }

    public boolean containsProblemOfType(String type) {
        return this.stream().anyMatch(problem -> type.equals(problem.type));
    }
}
