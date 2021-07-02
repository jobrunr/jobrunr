package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public abstract class VisitLocalVariableInstruction extends AbstractJVMInstruction {

    protected int variable;

    protected VisitLocalVariableInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load(int variable) {
        this.variable = variable;
        jobDetailsBuilder.pushInstructionOnStack(this);
    }

    @Override
    public Object invokeInstruction() {
        return jobDetailsBuilder.getLocalVariable(variable);
    }

}
