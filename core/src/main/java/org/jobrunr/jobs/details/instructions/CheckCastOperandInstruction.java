package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public class CheckCastOperandInstruction extends VisitTypeInstruction {

    public CheckCastOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public void load(String type) {
        // not needed
    }

    @Override
    public Object invokeInstruction() {
        return null;
    }
}
