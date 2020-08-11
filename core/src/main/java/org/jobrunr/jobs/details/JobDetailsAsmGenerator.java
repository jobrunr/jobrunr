package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.instructions.AllJVMInstructions;
import org.jobrunr.jobs.details.instructions.InvokeDynamicInstruction;
import org.jobrunr.jobs.details.instructions.LdcInstruction;
import org.jobrunr.jobs.details.instructions.SingleIntOperandInstruction;
import org.jobrunr.jobs.details.instructions.VisitFieldInstruction;
import org.jobrunr.jobs.details.instructions.VisitLocalVariableInstruction;
import org.jobrunr.jobs.details.instructions.VisitMethodInstruction;
import org.jobrunr.jobs.details.instructions.VisitTypeInstruction;
import org.jobrunr.jobs.details.instructions.ZeroOperandInstruction;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobRunrJob;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.invoke.SerializedLambda;

import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class JobDetailsAsmGenerator implements JobDetailsGenerator {
    private final SerializedLambdaConverter serializedLambdaConverter;

    public JobDetailsAsmGenerator() {
        this.serializedLambdaConverter = new SerializedLambdaConverter();
    }

    @Override
    public <T extends JobRunrJob> JobDetails toJobDetails(T lambda) {
        if (!lambda.getClass().isSynthetic()) {
            throw new IllegalArgumentException("Please provide a lambda expression (e.g. BackgroundJob.enqueue(() -> myService.doWork()) instead of an actual implementation.");
        } else {
            SerializedLambda serializedLambda = serializedLambdaConverter.toSerializedLambda(lambda);
            JobDetailsFinder jobDetailsFinder = new JobDetailsFinder(serializedLambda);
            return findJobDetailsInByteCode(lambda, jobDetailsFinder);
        }
    }

    @Override
    public <TItem> JobDetails toJobDetails(TItem input, JobLambdaFromStream<TItem> lambda) {
        SerializedLambda serializedLambda = serializedLambdaConverter.toSerializedLambda(lambda);
        JobDetailsFinder jobDetailsFinder = new JobDetailsFinder(serializedLambda, input);
        return findJobDetailsInByteCode(lambda, jobDetailsFinder);
    }

    @Override
    public <TService, TItem> JobDetails toJobDetails(TItem input, IocJobLambdaFromStream<TService, TItem> lambda) {
        SerializedLambda serializedLambda = serializedLambdaConverter.toSerializedLambda(lambda);
        JobDetailsFinder jobDetailsFinder = new JobDetailsFinder(serializedLambda, null, input);
        return findJobDetailsInByteCode(lambda, jobDetailsFinder);
    }

    private JobDetails findJobDetailsInByteCode(Object lambda, JobDetailsFinder jobDetailsFinder) {
        try {
            ClassReader parser = new ClassReader(JobDetailsGeneratorUtils.getClassContainingLambdaAsInputStream(lambda));
            parser.accept(jobDetailsFinder, ClassReader.SKIP_FRAMES);
            return jobDetailsFinder.getJobDetails();
        } catch (IOException e) {
            throw shouldNotHappenException(e);
        }
    }

    private static class JobDetailsFinder extends ClassVisitor {

        private final SerializedLambda serializedLambda;
        private final JobDetailsFinderContext jobDetailsFinderContext;

        private JobDetailsFinder(SerializedLambda serializedLambda, Object... params) {
            super(Opcodes.ASM7);
            this.serializedLambda = serializedLambda;
            this.jobDetailsFinderContext = new JobDetailsFinderContext(serializedLambda, params);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (serializedLambda.getImplMethodName().startsWith("lambda$") && name.equals(serializedLambda.getImplMethodName())) {
                return new MethodVisitor(Opcodes.ASM7) {

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        VisitFieldInstruction instruction = AllJVMInstructions.get(opcode, jobDetailsFinderContext);
                        instruction.load(owner, name, descriptor);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                        InvokeDynamicInstruction instruction = AllJVMInstructions.get(Opcodes.INVOKEDYNAMIC, jobDetailsFinderContext);
                        instruction.load(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        VisitMethodInstruction visitMethodInstruction = AllJVMInstructions.get(opcode, jobDetailsFinderContext);
                        visitMethodInstruction.load(owner, name, descriptor, isInterface);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        ZeroOperandInstruction zeroOperandInstruction = AllJVMInstructions.get(opcode, jobDetailsFinderContext);
                        zeroOperandInstruction.load();
                    }

                    @Override
                    public void visitVarInsn(int opcode, int var) {
                        VisitLocalVariableInstruction instruction = AllJVMInstructions.get(opcode, jobDetailsFinderContext);
                        instruction.load(var);
                    }

                    @Override
                    public void visitIntInsn(int opcode, int operand) {
                        SingleIntOperandInstruction singleIntOperandInstruction = AllJVMInstructions.get(opcode, jobDetailsFinderContext);
                        singleIntOperandInstruction.load(operand);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        LdcInstruction ldcInstruction = AllJVMInstructions.get(Opcodes.LDC, jobDetailsFinderContext);
                        ldcInstruction.load(value);
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        VisitTypeInstruction instruction = AllJVMInstructions.get(opcode, jobDetailsFinderContext);
                        instruction.load(type);
                    }
                };
            } else {
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }

        public JobDetails getJobDetails() {
            return jobDetailsFinderContext.getJobDetails();
        }
    }
}
