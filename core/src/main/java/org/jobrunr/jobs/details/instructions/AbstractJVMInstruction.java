package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public abstract class AbstractJVMInstruction {

    public static final Object DO_NOT_PUT_ON_STACK = new Object();

    protected final JobDetailsBuilder jobDetailsBuilder;

    protected AbstractJVMInstruction(JobDetailsBuilder jobDetailsBuilder) {
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
