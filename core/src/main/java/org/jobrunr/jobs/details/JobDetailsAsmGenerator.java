package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.instructions.*;
import org.jobrunr.jobs.lambdas.*;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getJavaClassContainingLambdaAsInputStream;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getKotlinClassContainingLambdaAsInputStream;
import static org.jobrunr.jobs.details.SerializedLambdaConverter.toSerializedLambda;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromField;

public class JobDetailsAsmGenerator implements JobDetailsGenerator {

    @Override
    public JobDetails toJobDetails(JobLambda lambda) {
        if (isKotlinLambda(lambda)) {
            if(isKotlin14Lambda(lambda)) {
                System.out.println("Is kotlin 1.4 lambda");
            } else if (isKotlin15Lambda(lambda)) {
                System.out.println("Is kotlin 1.5 lambda");
            }
            return new KotlinJobDetailsFinder(isKotlin15Lambda(lambda), lambda).getJobDetails();
        } else {
            return new JavaJobDetailsFinder(lambda, toSerializedLambda(lambda)).getJobDetails();
        }
    }

    @Override
    public JobDetails toJobDetails(IocJobLambda lambda) {
        if (isKotlinLambda(lambda)) {
            if(isKotlin14Lambda(lambda)) {
                System.out.println("Is kotlin 1.4 lambda");
            } else if (isKotlin15Lambda(lambda)) {
                System.out.println("Is kotlin 1.5 lambda");
            }
            return new KotlinJobDetailsFinder(isKotlin15Lambda(lambda), lambda, new Object()).getJobDetails();
        } else {
            return new JavaJobDetailsFinder(lambda, toSerializedLambda(lambda)).getJobDetails();
        }
    }

    @Override
    public <T> JobDetails toJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda) {
        if (isKotlinLambda(lambda)) {
            return new KotlinJobDetailsFinder(isKotlin15Lambda(lambda), lambda, itemFromStream).getJobDetails();
        } else {
            return new JavaJobDetailsFinder(lambda, toSerializedLambda(lambda), itemFromStream).getJobDetails();
        }
    }

    @Override
    public <S, T> JobDetails toJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda) {
        if (isKotlinLambda(lambda)) {
            // why new Object(): it represents the item injected when we run the IocJobLambdaFromStream function
            return new KotlinJobDetailsFinder(isKotlin15Lambda(lambda), lambda, new Object(), itemFromStream).getJobDetails();
        } else {
            // why null: it represents the item injected when we run the IocJobLambdaFromStream function
            return new JavaJobDetailsFinder(lambda, toSerializedLambda(lambda), null, itemFromStream).getJobDetails();
        }
    }

    private <T extends JobRunrJob> boolean isKotlinLambda(T lambda) {
        return !lambda.getClass().isSynthetic() && stream(lambda.getClass().getAnnotations()).map(Annotation::annotationType).anyMatch(annotationType -> annotationType.getName().equals("kotlin.Metadata"));
    }

    private <T extends JobRunrJob> boolean isKotlin14Lambda(T lambda) {
        final Annotation annotation = stream(lambda.getClass().getAnnotations()).filter(a -> a.annotationType().getName().equals("kotlin.Metadata")).findFirst().orElseThrow(() -> new IllegalStateException("Did not find kotlin metadata annotation"));
        return annotation.toString().contains("mv={1, 4");
    }

    private <T extends JobRunrJob> boolean isKotlin15Lambda(T lambda) {
        final Annotation annotation = stream(lambda.getClass().getAnnotations()).filter(a -> a.annotationType().getName().equals("kotlin.Metadata")).findFirst().orElseThrow(() -> new IllegalStateException("Did not find kotlin metadata annotation"));
        return annotation.toString().contains("mv={1, 5");
    }

    private static class JavaJobDetailsFinder extends JobDetailsFinder {

        private final JobRunrJob jobRunrJob;
        private final SerializedLambda serializedLambda;

        private JavaJobDetailsFinder(JobRunrJob jobRunrJob, SerializedLambda serializedLambda, Object... params) {
            super(new JavaJobDetailsFinderContext(serializedLambda, params));
            this.jobRunrJob = jobRunrJob;
            this.serializedLambda = serializedLambda;
            parse(getClassContainingLambdaAsInputStream());
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

        private int methodCounter = 0;
        private boolean oneFive;
        private JobRunrJob jobRunrJob;

        private String nestedKotlinClassWithMethodReference;

        private KotlinJobDetailsFinder(boolean oneFive, JobRunrJob jobRunrJob, Object... params) {
            super(new KotlinJobDetailsFinderContext(jobRunrJob, params));
            this.oneFive = oneFive;
            this.jobRunrJob = jobRunrJob;
            parse(getClassContainingLambdaAsInputStream());
        }

        @Override
        protected boolean isLambdaContainingJobDetails(String name) {
            if (name.equals("accept") || name.equals("invoke")) {
                methodCounter++;
            }
            if(oneFive) {
                return name.equals("run") || ((name.equals("accept") || name.equals("invoke")) && methodCounter == 1);
            } else {
                return name.equals("run") || ((name.equals("accept") || name.equals("invoke")) && methodCounter == 2);
            }
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if (access == 0x1018) {
                this.nestedKotlinClassWithMethodReference = name;
            }
        }

        @Override
        protected InputStream getClassContainingLambdaAsInputStream() {
            return getKotlinClassContainingLambdaAsInputStream(jobRunrJob);
        }

        @Override
        protected void parse(InputStream inputStream) {
            Optional<Field> field = ReflectionUtils.findField(jobRunrJob.getClass(), "function");
            if (field.isPresent()) {
                getJobDetailsFromKotlinFunction(field.get());
            } else {
                super.parse(inputStream);
                parseNestedClassIfItIsAMethodReference();
            }
        }

        private void getJobDetailsFromKotlinFunction(Field field) {
            Object function = getValueFromField(field, jobRunrJob);
            //Field owner = ReflectionUtils.getField(function.getClass(), "owner");
            Field receiver = ReflectionUtils.getField(function.getClass(), "receiver");
            Field name = ReflectionUtils.getField(function.getClass(), "name");
            Class<?> receiverClass = getValueFromField(receiver, function).getClass();
            String methodName = cast(getValueFromField(name, function));
            jobDetailsFinderContext.setClassName(receiverClass.getName());
            jobDetailsFinderContext.setMethodName(methodName);
        }

        private void parseNestedClassIfItIsAMethodReference() {
            if (nestedKotlinClassWithMethodReference != null) {
                String location = "/" + nestedKotlinClassWithMethodReference + ".class";
                super.parse(jobRunrJob.getClass().getResourceAsStream(location));
                while (jobDetailsFinderContext.getInstructions().size() > 1) {
                    jobDetailsFinderContext.pollFirstInstruction();
                }
            }
        }
    }

    private static abstract class JobDetailsFinder extends ClassVisitor {

        protected final JobDetailsFinderContext jobDetailsFinderContext;

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
            return jobDetailsFinderContext.getJobDetails();
        }

        protected void parse(InputStream inputStream) {
            try {
                ClassReader parser = new ClassReader(inputStream);
                parser.accept(this, ClassReader.SKIP_FRAMES);
            } catch (IOException e) {
                throw shouldNotHappenException(e);
            }
        }
    }
}
