package org.jobrunr.jobs.details.instructions;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.details.JobDetailsFinderContext;
import org.objectweb.asm.Handle;

public class InvokeDynamicInstruction extends AbstractJVMInstruction {

    private String name;
    private String descriptor;
    private Handle bootstrapMethodHandle;
    private Object[] bootstrapMethodArguments;

    public InvokeDynamicInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        jobDetailsBuilder.pushInstructionOnStack(this);
        this.name = name;
        this.descriptor = descriptor;
        this.bootstrapMethodHandle = bootstrapMethodHandle;
        this.bootstrapMethodArguments = bootstrapMethodArguments;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public Handle getBootstrapMethodHandle() {
        return bootstrapMethodHandle;
    }

    public Object[] getBootstrapMethodArguments() {
        return bootstrapMethodArguments;
    }

    @Override
    public Object invokeInstruction() {
        if ("makeConcatWithConstants".equals(name)) {
            String result = bootstrapMethodArguments[0].toString();

            while (result.contains("\u0001")) {
                result = replaceLast(result, "\u0001", jobDetailsBuilder.pollLastInstruction().invokeInstruction().toString());
            }
            return result;
        }
        throw JobRunrException.shouldNotHappenException("");
    }

    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }
}
