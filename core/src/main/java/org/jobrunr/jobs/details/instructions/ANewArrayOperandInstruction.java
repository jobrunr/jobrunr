package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

import java.lang.reflect.Array;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class ANewArrayOperandInstruction extends VisitTypeInstruction {

    public ANewArrayOperandInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        Integer arraySize = (Integer) jobDetailsBuilder.getStack().pollLast();
        final Object[] result = (Object[]) Array.newInstance(toClass(toFQClassName(type)), arraySize);
        for (int i = 0; i < arraySize; i++) {
            final Object arrayIndex = jobDetailsBuilder.pollFirstInstruction().invokeInstruction();
            final Object arrayItem = jobDetailsBuilder.pollFirstInstruction().invokeInstruction();
            result[i] = arrayItem;
        }
        return result;
    }
}
