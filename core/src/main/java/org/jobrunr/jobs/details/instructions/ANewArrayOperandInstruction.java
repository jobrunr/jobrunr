package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

import java.lang.reflect.Array;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class ANewArrayOperandInstruction extends VisitTypeInstruction {

    public ANewArrayOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        Integer arraySize = (Integer) jobDetailsBuilder.getStack().pollLast();
        return Array.newInstance(toClass(toFQClassName(type)), arraySize);
    }
}
