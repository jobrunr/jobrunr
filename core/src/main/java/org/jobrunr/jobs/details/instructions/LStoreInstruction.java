package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public class LStoreInstruction extends StoreVariableInstruction {

    public LStoreInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        super.invokeInstruction();
        jobDetailsBuilder.addLocalVariable(null); //why: If the local variable at index is of type double or long, it occupies both index and index + 1. See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html
        return null;
    }
}
