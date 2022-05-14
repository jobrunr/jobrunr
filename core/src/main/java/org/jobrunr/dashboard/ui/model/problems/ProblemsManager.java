package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.storage.StorageProvider;

import java.util.HashMap;
import java.util.Map;

public class ProblemsManager {

    private final StorageProvider storageProvider;
    private final Problems problems;
    private final Map<String, ProblemHandler> problemHandlers;

    public ProblemsManager(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
        this.problems = new Problems();
        this.problemHandlers = initProblemHandlers();
    }

    private Map<String, ProblemHandler> initProblemHandlers() {
        Map<String, ProblemHandler> result = new HashMap<>();
        result.put(ScheduledJobsNotFoundProblem.PROBLEM_TYPE, new ScheduledJobsNotFoundProblemHandler(problems, storageProvider));
        result.put(SevereJobRunrExceptionProblem.PROBLEM_TYPE, new SevereJobRunrExceptionProblemHandler(problems, storageProvider));
        result.put(CpuAllocationIrregularityProblem.PROBLEM_TYPE, new CpuAllocationIrregularityProblemHandler(problems, storageProvider));
        result.put(PollIntervalInSecondsTimeBoxIsTooSmallProblem.PROBLEM_TYPE, new PollIntervalInSecondsTimeBoxIsTooSmallProblemHandler(problems, storageProvider));
        result.put(NewJobRunrVersionProblem.PROBLEM_TYPE, new NewJobRunrVersionProblemHandler(problems, storageProvider));
        return result;
    }

    public Problems getProblems() {
        return problems;
    }

    public void dismissProblemOfType(String param) {
        if (problemHandlers.containsKey(param)) {
            problemHandlers.get(param).dismiss();
        } else {
            throw new IllegalArgumentException("Unknown problem of type '" + param + "'");
        }
    }
}
