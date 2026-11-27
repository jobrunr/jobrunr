package org.jobrunr.jobs.details.instructions;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.util.List;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesFromDescriptor;

public class InvokeDynamicInstruction extends AbstractJVMInstruction {

    private String name;
    private String descriptor;
    private Object[] bootstrapMethodArguments;

    public InvokeDynamicInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load(String name, String descriptor, Object... bootstrapMethodArguments) {
        jobDetailsBuilder.pushInstructionOnStack(this);
        this.name = name;
        this.descriptor = descriptor;
        this.bootstrapMethodArguments = bootstrapMethodArguments;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public Object invokeInstruction() {
        if ("makeConcatWithConstants".equals(name)) {
            String result = bootstrapMethodArguments[0].toString();
            final List<Class<?>> paramTypes = findParamTypesFromDescriptor(descriptor);
            while (result.contains("\u0001") && !paramTypes.isEmpty()) {
                final Class<?> paramType = paramTypes.remove(paramTypes.size() - 1);
                final Object value = ReflectionUtils.autobox(jobDetailsBuilder.getStack().pollLast(), paramType);
                result = replaceLast(result, "\u0001", value.toString());
            }
            return result;
        }
        throw JobRunrException.shouldNotHappenException("Unknown INVOKEDYNAMIC instruction: " + name);
    }

    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }
}
