package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public abstract class AbstractJVMInstruction {

    protected final JobDetailsFinderContext jobDetailsBuilder;

    public AbstractJVMInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        this.jobDetailsBuilder = jobDetailsBuilder;
    }

    public abstract Object invokeInstruction();

    public void invokeInstructionAndPushOnStack() {
        Object result = invokeInstruction();
        jobDetailsBuilder.getStack().add(result);
    }
}
