package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class StoreVariableInstruction extends VisitLocalVariableInstruction {

    public StoreVariableInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public int getVariable() {
        return variable;
    }

    @Override
    public void invokeInstructionAndPushOnStack() {
        invokeInstruction(); // nothing to put on the stack
    }

    @Override
    public Object invokeInstruction() {
        jobDetailsBuilder.addLocalVariable(jobDetailsBuilder.getStack().pollLast());
        return null;
    }
}
