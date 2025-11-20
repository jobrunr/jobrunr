package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jspecify.annotations.Nullable;

public class StoreVariableInstruction extends VisitLocalVariableInstruction {

    public StoreVariableInstruction(JobDetailsBuilder jobDetailsBuilder) {
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
    public @Nullable Object invokeInstruction() {
        jobDetailsBuilder.addLocalVariable(jobDetailsBuilder.getStack().pollLast());
        return null;
    }
}
