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
    public Object invokeInstruction() {
        final AbstractJVMInstruction jvmInstruction = jobDetailsBuilder.getInstructions().remove(nbrInStack - 1);
        return jvmInstruction.invokeInstruction();
    }
}
