package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public abstract class AbstractJVMInstruction {

    public static Object DO_NOT_PUT_ON_STACK = new Object();

    protected final JobDetailsFinderContext jobDetailsBuilder;

    protected AbstractJVMInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        this.jobDetailsBuilder = jobDetailsBuilder;
    }

    public abstract Object invokeInstruction();

    public void invokeInstructionAndPushOnStack() {
        Object result = invokeInstruction();
        if (result != DO_NOT_PUT_ON_STACK) {
            jobDetailsBuilder.getStack().add(result);
        }
    }
}
