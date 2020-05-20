package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public abstract class AbstractJVMInstruction {

    protected final JobDetailsFinderContext jobDetailsBuilder;

    protected int nbrInStack;

    public AbstractJVMInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        this.jobDetailsBuilder = jobDetailsBuilder;
    }

    public abstract Object invokeInstruction();

    public void setNbrInStack(int nbrInStack) {
        this.nbrInStack = nbrInStack;
    }
}
