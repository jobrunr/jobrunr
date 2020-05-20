package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class LConst1OperandInstruction extends ZeroOperandInstruction {

    public LConst1OperandInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        return 1L;
    }
}
