package org.jobrunr.dashboard.ui.model.problems;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemsTest {

    Problems problems = new Problems();

    @Test
    void testAddProblem() {
        problems.addProblem(new CpuAllocationIrregularityProblem(new ArrayList<>()));
        problems.addProblem(new CpuAllocationIrregularityProblem(new ArrayList<>()));

        assertThat(problems).hasSize(1);
    }

    @Test
    void testRemoveProblemsOfType() {
        problems.addProblem(new CpuAllocationIrregularityProblem(new ArrayList<>()));
        assertThat(problems).hasSize(1);

        problems.removeProblemsOfType(CpuAllocationIrregularityProblem.PROBLEM_TYPE);

        assertThat(problems).isEmpty();
    }

    @Test
    void testContainsProblemOfType() {
        problems.addProblem(new CpuAllocationIrregularityProblem(new ArrayList<>()));

        assertThat(problems.containsProblemOfType(CpuAllocationIrregularityProblem.PROBLEM_TYPE)).isTrue();
        assertThat(problems.containsProblemOfType(PollIntervalInSecondsTimeBoxIsTooSmallProblem.PROBLEM_TYPE)).isFalse();
    }

}