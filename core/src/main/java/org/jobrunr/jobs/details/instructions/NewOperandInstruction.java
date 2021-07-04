package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public class NewOperandInstruction extends VisitTypeInstruction {

    public NewOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
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
