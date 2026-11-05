package org.jobrunr.jobs.details.finders;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jobrunr.utils.JarUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;

abstract class AbstractAsmJobDetailsFinder extends ClassVisitor {

    protected final JobDetailsBuilder jobDetailsBuilder;
    private final AsmInstructionsMethodVisitor asmInstructionsMethodVisitor;

    protected AbstractAsmJobDetailsFinder(JobDetailsBuilder jobDetailsBuilder) {
        super(Opcodes.ASM7);
        this.jobDetailsBuilder = jobDetailsBuilder;
        this.asmInstructionsMethodVisitor = new AsmInstructionsMethodVisitor(jobDetailsBuilder);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (isLambdaContainingJobDetails(name)) {
            return asmInstructionsMethodVisitor;
        } else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    protected abstract boolean isLambdaContainingJobDetails(String name);

    public JobDetails getJobDetails() {
        return jobDetailsBuilder.getJobDetails();
    }

    protected void parse(InputStream inputStream) throws IOException {
        try {
            ClassReader parser = new ClassReader(inputStream);
            parser.accept(this, ClassReader.SKIP_FRAMES);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().startsWith("Unsupported class file")) {
                String requiredAsmVersion = JarUtils.getManifestAttributeValue(JobRunr.class, "Minimum-ASM-Version");
                String actualAsmVersion = JarUtils.getVersion(Opcodes.class);
                throw new IllegalArgumentException("JobRunr needs (and automatically adds) ASM " + requiredAsmVersion + " as a transitive dependency but you have ASM " + actualAsmVersion + " on the classpath.", e);
            }
            throw e;
        }
    }

}
