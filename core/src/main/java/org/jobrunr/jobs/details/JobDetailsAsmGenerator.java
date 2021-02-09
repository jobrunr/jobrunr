package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.instructions.*;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobRunrJob;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.invoke.SerializedLambda;

import static java.util.Arrays.stream;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getJavaClassContainingLambdaAsInputStream;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getKotlinClassContainingLambdaAsInputStream;
import static org.jobrunr.jobs.details.SerializedLambdaConverter.toSerializedLambda;

public class JobDetailsAsmGenerator implements JobDetailsGenerator {

    @Override
    public <T extends JobRunrJob> JobDetails toJobDetails(T lambda) {
        if (isKotlinLambda(lambda)) {
            return new KotlinJobDetailsFinder(lambda).getJobDetails();
        } else {
            return new JavaJobDetailsFinder(lambda, toSerializedLambda(lambda)).getJobDetails();
        }
    }

    @Override
    public <T> JobDetails toJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda) {
        return new JavaJobDetailsFinder(lambda, toSerializedLambda(lambda), itemFromStream).getJobDetails();
    }

    @Override
    public <S, T> JobDetails toJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda) {
        return new JavaJobDetailsFinder(lambda, toSerializedLambda(lambda), null, itemFromStream).getJobDetails();
    }

    private <T extends JobRunrJob> boolean isKotlinLambda(T lambda) {
        return !lambda.getClass().isSynthetic() && stream(lambda.getClass().getAnnotations()).map(Annotation::annotationType).anyMatch(annotationType -> annotationType.getName().equals("kotlin.Metadata"));
    }

    private static class JavaJobDetailsFinder extends JobDetailsFinder {

        private final JobRunrJob jobRunrJob;
        private final SerializedLambda serializedLambda;

        private JavaJobDetailsFinder(JobRunrJob jobRunrJob, SerializedLambda serializedLambda, Object... params) {
            super(new JavaJobDetailsFinderContext(serializedLambda, params));
            this.jobRunrJob = jobRunrJob;
            this.serializedLambda = serializedLambda;
        }

        @Override
        protected boolean isLambdaContainingJobDetails(String name) {
            return serializedLambda.getImplMethodName().startsWith("lambda$") && name.equals(serializedLambda.getImplMethodName());
        }

        @Override
        protected InputStream getClassContainingLambdaAsInputStream() {
            return getJavaClassContainingLambdaAsInputStream(jobRunrJob);
        }
    }

    private static class KotlinJobDetailsFinder extends JobDetailsFinder {

        private JobRunrJob jobRunrJob;

        private KotlinJobDetailsFinder(JobRunrJob jobRunrJob) {
            super(new KotlinJobDetailsFinderContext(jobRunrJob));
            this.jobRunrJob = jobRunrJob;
        }

        @Override
        protected boolean isLambdaContainingJobDetails(String name) {
            return name.equals("run");
        }

        @Override
        protected InputStream getClassContainingLambdaAsInputStream() {
            return getKotlinClassContainingLambdaAsInputStream(jobRunrJob);
        }
    }

    private static abstract class JobDetailsFinder extends ClassVisitor {

        private final JobDetailsFinderContext jobDetailsFinderContext;

        private JobDetailsFinder(JobDetailsFinderContext jobDetailsFinderContext) {
            super(Opcodes.ASM7);
            this.jobDetailsFinderContext = jobDetailsFinderContext;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (isLambdaContainingJobDetails(name)) {
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

        protected abstract boolean isLambdaContainingJobDetails(String name);

        protected abstract InputStream getClassContainingLambdaAsInputStream();

        public JobDetails getJobDetails() {
            try {
                ClassReader parser = new ClassReader(getClassContainingLambdaAsInputStream());
                parser.accept(this, ClassReader.SKIP_FRAMES);
                return jobDetailsFinderContext.getJobDetails();
            } catch (IOException e) {
                throw shouldNotHappenException(e);
            }
        }
    }
}
