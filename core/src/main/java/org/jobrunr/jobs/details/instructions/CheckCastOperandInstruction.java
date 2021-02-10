package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class CheckCastOperandInstruction extends VisitTypeInstruction {

    public CheckCastOperandInstruction(JobDetailsFinderContext jobDetailsBuilder) {
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
