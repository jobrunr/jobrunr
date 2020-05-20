package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public abstract class VisitLocalVariableInstruction extends AbstractJVMInstruction {

    protected int variable;

    public VisitLocalVariableInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load(int var) {
        this.variable = var;
        jobDetailsBuilder.pushInstructionOnStack(this);
    }

    @Override
    public Object invokeInstruction() {
        return jobDetailsBuilder.getLocalVariable(variable);
    }
}
