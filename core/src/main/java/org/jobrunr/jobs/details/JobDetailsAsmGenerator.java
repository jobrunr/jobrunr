package org.jobrunr.jobs.details;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobContext;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.createObjectViaConstructor;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.createObjectViaMethod;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.createObjectViaStaticMethod;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesForDescriptor;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.findParamTypesForDescriptorAsArray;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.isClassAssignableToObject;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

// this is not the most beautiful code but it is thoroughly tested. Are there better ways to capture the actual lambda? Serialized lambda does not work as the deployed server code must then contain the scheduling code.
public class JobDetailsAsmGenerator implements JobDetailsGenerator {
    private final SerializedLambdaConverter serializedLambdaConverter;

    public JobDetailsAsmGenerator() {
        this.serializedLambdaConverter = new SerializedLambdaConverter();
    }

    @Override
    public <T extends Serializable> JobDetails toJobDetails(T lambda) {
        SerializedLambda serializedLambda = serializedLambdaConverter.toSerializedLambda(lambda);
        JobDetailsFinder jobDetailsFinder = new JobDetailsFinder(lambda, serializedLambda);
        return findJobDetailsInByteCode(lambda, jobDetailsFinder);
    }

    @Override
    public <TItem> JobDetails toJobDetails(TItem input, JobLambdaFromStream<TItem> lambda) {
        SerializedLambda serializedLambda = serializedLambdaConverter.toSerializedLambda(lambda);
        JobDetailsFinder jobDetailsFinder = new JobDetailsFinder(lambda, serializedLambda, input);
        return findJobDetailsInByteCode(lambda, jobDetailsFinder);
    }

    @Override
    public <TService, TItem> JobDetails toJobDetails(TItem input, IocJobLambdaFromStream<TService, TItem> lambda) {
        SerializedLambda serializedLambda = serializedLambdaConverter.toSerializedLambda(lambda);
        JobDetailsFinder jobDetailsFinder = new JobDetailsFinder(lambda, serializedLambda, input);
        return findJobDetailsInByteCode(lambda, jobDetailsFinder);
    }

    private JobDetails findJobDetailsInByteCode(Object lambda, JobDetailsFinder jobDetailsFinder) {
        try {
            ClassReader parser = new ClassReader(JobDetailsGeneratorUtils.getClassContainingLambdaAsInputStream(lambda));
            parser.accept(jobDetailsFinder, ClassReader.SKIP_FRAMES);
            return jobDetailsFinder.getJobDetails();
        } catch (IOException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private static class JobDetailsFinder extends ClassVisitor {

        private final Object lambda;
        private final SerializedLambda serializedLambda;
        private final boolean streaming;

        private String className;
        private String staticFieldName;
        private String methodName;
        private List<Object> params;
        private List<Class> paramTypes;

        public JobDetailsFinder(Object lambda, SerializedLambda serializedLambda) {
            this(lambda, serializedLambda, new ArrayList<>());
        }

        public JobDetailsFinder(Object lambda, SerializedLambda serializedLambda, Object parameter) {
            this(lambda, serializedLambda, new ArrayList<>(asList(parameter)));
        }

        private JobDetailsFinder(Object lambda, SerializedLambda serializedLambda, List<Object> params) {
            super(Opcodes.ASM7);
            this.lambda = lambda;
            this.serializedLambda = serializedLambda;
            this.params = params;
            this.streaming = !params.isEmpty();
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (name.equals(serializedLambda.getImplMethodName())) {
                return new MethodVisitor(Opcodes.ASM7) {

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        String fqClassName = toFQClassName(owner);
                        if (Opcodes.GETSTATIC == opcode) {
                            if (JobContext.class.getName().equals(fqClassName)) {
                                params.add(org.jobrunr.jobs.JobContext.Null);
                            } else {
                                className = fqClassName;
                                staticFieldName = name;
                            }
                        }
                        super.visitFieldInsn(opcode, owner, name, descriptor);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                        if ("makeConcatWithConstants".equals(name)) {
                            String result = bootstrapMethodArguments[0].toString();

                            while (result.contains("\u0001")) {
                                result = result.replaceFirst("\u0001", params.remove(0).toString());
                            }
                            params.add(result);
                        } else {
                            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
                        }

                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        String fqClassName = toFQClassName(owner);
                        if (Opcodes.INVOKEVIRTUAL == opcode) {
                            if (paramsContainObjectOfType(fqClassName)) {
                                Class[] paramTypes = findParamTypesForDescriptorAsArray(descriptor);
                                params.add(createObjectViaMethod(getObject(fqClassName), name, paramTypes, getParameters(paramTypes)));
                            } else if (staticFieldName == null) {
                                if (className != null)
                                    throw new JobRunrException("Are you trying to enqueue a multiline lambda? This is not yet supported. If not, please create a bug report (if possible, provide the code to reproduce this and the stacktrace)");
                                className = fqClassName;
                                methodName = name;
                                paramTypes = findParamTypesForDescriptor(descriptor);
                            } else if (staticFieldName != null) {
                                methodName = name;
                                paramTypes = findParamTypesForDescriptor(descriptor);
                            }
                        } else if (Opcodes.INVOKESTATIC == opcode) {
                            Class[] paramTypes = findParamTypesForDescriptorAsArray(descriptor);
                            params.add(createObjectViaStaticMethod(fqClassName, name, paramTypes, getParameters(paramTypes)));
                        } else if (Opcodes.INVOKESPECIAL == opcode) {
                            Class[] paramTypes = findParamTypesForDescriptorAsArray(descriptor);
                            params.add(createObjectViaConstructor(fqClassName, paramTypes, getParameters(paramTypes)));
                        } else if (Opcodes.INVOKEDYNAMIC == opcode) {
                            //System.out.println("look here");
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (Opcodes.ICONST_0 == opcode) params.add(0);
                        else if (Opcodes.ICONST_1 == opcode) params.add(1);
                        else if (Opcodes.ICONST_2 == opcode) params.add(2);
                        else if (Opcodes.ICONST_3 == opcode) params.add(3);
                        else if (Opcodes.ICONST_4 == opcode) params.add(4);
                        else if (Opcodes.ICONST_5 == opcode) params.add(5);
                        super.visitInsn(opcode);
                    }

                    @Override
                    public void visitVarInsn(int opcode, int var) {
                        if (isRunnable() && var == 0) return;
                        if (streaming && var == 0) return;

                        int argToCapture = isBiConsumerJob() ? var - 1 : var;
                        if (Opcodes.ALOAD == opcode || Opcodes.ILOAD == opcode) {
                            if (serializedLambda.getCapturedArgCount() > argToCapture) {
                                Object capturedArg = serializedLambda.getCapturedArg(argToCapture);
                                params.add(capturedArg);
                            }
                        }
                        super.visitVarInsn(opcode, var);
                    }

                    @Override
                    public void visitIntInsn(int opcode, int operand) {
                        if (Opcodes.BIPUSH == opcode) params.add(operand);
                        super.visitIntInsn(opcode, operand);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        params.add(value);
                        super.visitLdcInsn(value);
                    }
                };
            } else {
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }

        public JobDetails getJobDetails() {
            if (className == null) { // method being called is a method reference
                return new JobDetails(toFQClassName(serializedLambda.getImplClass()), null, serializedLambda.getImplMethodName(), new ArrayList<>());
            }
            return new JobDetails(className, staticFieldName, methodName, getJobParameters());
        }

        private List<JobParameter> getJobParameters() {
            if (paramTypes.size() > params.size()) throw JobRunrException.shouldNotHappenException("The number of parameterTypes and parameters does not match.");

            params.removeIf(param -> paramTypes.stream().noneMatch(paramType -> isClassAssignableToObject(paramType, param)));

            return IntStream.range(0, paramTypes.size()).mapToObj(i -> toJobParameter(paramTypes.get(i), params.get(i))).collect(toList());
        }

        private JobParameter toJobParameter(Class paramType, Object param) {
            if (isClassAssignableToObject(paramType, param)) {
                if (boolean.class.equals(paramType) && Integer.class.equals(param.getClass())) return new JobParameter(paramType, ((Integer) param) > 0);
                return new JobParameter(paramType, param);
            } else {
                throw JobRunrException.shouldNotHappenException(new IllegalStateException("The found parameter types do not match the parameters."));
            }
        }

        private boolean paramsContainObjectOfType(String fqn) {
            return params.stream().anyMatch(param -> fqn.equals(param.getClass().getName()));
        }

        private Object getObject(String fqn) {
            Object result = null;
            for (Object param : params) {
                if (fqn.equals(param.getClass().getName())) {
                    result = param;
                    break;
                }
            }
            params.remove(result);
            return result;
        }

        private Object[] getParameters(Class[] paramTypes) {
            if (paramTypes.length == 0) return new Object[]{};

            List<Object> result = getParametersBasedOnParameterType(paramTypes);
            removeUsedParametersFromJobParameters(result);
            return result.toArray();
        }

        private List<Object> getParametersBasedOnParameterType(Class[] paramTypes) {
            List<Object> result = new ArrayList<>();
            for (Class clazz : paramTypes) {
                if (clazz.isArray()) {
                    List<Object> subResult = new ArrayList<>();
                    final List<Object> arrayParams = params.subList(params.indexOf(Integer.valueOf(0)), params.size());
                    Integer counter = 0;
                    for (Object param : arrayParams) {
                        if (counter.equals(param) && arrayParams.size() > (counter * 2 + 1) && isClassAssignableToObject(clazz.getComponentType(), arrayParams.get(counter * 2 + 1))) {
                            subResult.add(arrayParams.get(counter * 2 + 1));
                            counter++;
                        }
                    }
                    result.add(toCastedArray(clazz.getComponentType(), subResult));
                } else {
                    for (Object param : params) {
                        if (isClassAssignableToObject(clazz, param)) {
                            result.add(param);
                            break;
                        }
                    }
                }

            }
            return result;
        }

        private void removeUsedParametersFromJobParameters(List<Object> result) {
            for (Object o : result) {
                if (o.getClass().isArray()) {
                    List<Object> arrayAsListToRemove = new ArrayList<>();
                    int length = Array.getLength(o);
                    for (int i = 0; i < length; i++) {
                        Object arrayElement = Array.get(o, i);
                        arrayAsListToRemove.add(i);
                        arrayAsListToRemove.add(arrayElement);
                    }
                    params.removeAll(arrayAsListToRemove);
                } else {
                    params.remove(o);
                }
            }
        }

        private boolean isRunnable() {
            return lambda instanceof JobLambda;
        }

        private boolean isBiConsumerJob() {
            return lambda instanceof IocJobLambdaFromStream;
        }

        private static <T> T[] toCastedArray(Class<T> classToCastTo, Collection c) {
            return (T[]) c.toArray((T[]) Array.newInstance(classToCastTo, 0));
        }
    }
}
