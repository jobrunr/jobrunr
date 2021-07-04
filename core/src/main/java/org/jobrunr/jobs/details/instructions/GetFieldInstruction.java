package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getObjectViaField;

public class GetFieldInstruction extends VisitFieldInstruction {

    public GetFieldInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        return getObjectViaField(jobDetailsBuilder.getStack().pollLast(), name);
    }
}
