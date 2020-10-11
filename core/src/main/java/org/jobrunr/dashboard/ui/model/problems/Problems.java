package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jobrunr.utils.JobUtils.jobExists;

public class Problems extends ArrayList<Problem> {

    public Problems(StorageProvider storageProvider) {
        initProblems(storageProvider);
    }

    public void addProblem(Problem problem) {
        this.add(problem);
    }

    private void initProblems(StorageProvider storageProvider) {
        Set<String> jobsThatCannotBeFoundAnymore = storageProvider.getDistinctJobSignatures(StateName.SCHEDULED).stream().filter(jobSignature -> !jobExists(jobSignature)).collect(Collectors.toSet());
        if (!jobsThatCannotBeFoundAnymore.isEmpty()) {
            addProblem(new JobsNotFoundProblem(jobsThatCannotBeFoundAnymore));
        }
    }
}
