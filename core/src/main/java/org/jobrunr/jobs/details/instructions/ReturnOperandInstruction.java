package org.jobrunr.jobs.details.instructions;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class ReturnOperandInstruction extends ZeroOperandInstruction {

    public ReturnOperandInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        final AbstractJVMInstruction instruction = jobDetailsBuilder.pollLastInstruction();
        if (instruction instanceof JobDetailsInstruction) {
            return ((JobDetailsInstruction) instruction).getJobDetails();
        }
        throw JobRunrException.shouldNotHappenException("The last instruction was not a JobDetailsInstruction");
    }
}
