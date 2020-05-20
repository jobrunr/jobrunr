package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class IConst4OperandInstruction extends ZeroOperandInstruction {

    public IConst4OperandInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        return 4;
    }
}
