package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class IConst2OperandInstruction extends ZeroOperandInstruction {

    public IConst2OperandInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        return 2;
    }
}
