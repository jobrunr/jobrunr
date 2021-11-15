package org.jobrunr.jobs.details;

import org.jobrunr.jobs.lambdas.JobRunrJob;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getKotlinClassContainingLambdaAsInputStream;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQResource;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromField;

public class KotlinJobDetailsFinder extends AbstractJobDetailsFinder {

    private static final String INVOKE = "invoke";
    private static final String ACCEPT = "accept";
    private static final String RUN = "run";

    private enum KotlinVersion {
        ONE_FOUR,
        ONE_FIVE,
        ONE_SIX
    }

    private final JobRunrJob jobRunrJob;
    private int methodCounter = 0;
    private KotlinVersion kotlinVersion;

    private String nestedKotlinClassWithMethodReference;

    KotlinJobDetailsFinder(JobRunrJob jobRunrJob, Object... params) {
        super(new KotlinJobDetailsBuilder(jobRunrJob, params));
        this.jobRunrJob = jobRunrJob;
        parse(getClassContainingLambdaAsInputStream());
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return new AnnotationVisitor(Opcodes.ASM7) {
            @Override
            public void visit(String name, Object value) {
                if ("mv".equals(name)) {
                    int[] version = cast(value);
                    if (version[0] == 1 && version[1] == 4) {
                        kotlinVersion = KotlinVersion.ONE_FOUR;
                    } else if (version[0] == 1 && version[1] == 5) {
                        kotlinVersion = KotlinVersion.ONE_FIVE;
                    } else if (version[0] == 1 && version[1] == 6) {
                        kotlinVersion = KotlinVersion.ONE_SIX;
                    } else {
                        throw new UnsupportedOperationException("The Kotlin version " + version[0] + "." + version[1] + " is unsupported");
                    }
                }
            }
        };
    }

    @Override
    protected boolean isLambdaContainingJobDetails(String name) {
        if (name.equals(ACCEPT) || name.equals(INVOKE)) {
            methodCounter++;
        }
        if (KotlinVersion.ONE_FOUR.equals(kotlinVersion)) {
            return name.equals(RUN) || ((name.equals(ACCEPT) || name.equals(INVOKE)) && methodCounter == 2);
        } else {
            return name.equals(RUN) || ((name.equals(ACCEPT) || name.equals(INVOKE)) && methodCounter == 1);
        }
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (access == 0x1018 || access == 0x1000) {
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
        Field receiver = ReflectionUtils.getField(function.getClass(), "receiver");
        Field name = ReflectionUtils.getField(function.getClass(), "name");
        Class<?> receiverClass = getValueFromField(receiver, function).getClass();
        String methodName = cast(getValueFromField(name, function));
        jobDetailsBuilder.setClassName(receiverClass.getName());
        jobDetailsBuilder.setMethodName(methodName);
    }

    private void parseNestedClassIfItIsAMethodReference() {
        boolean isNestedKotlinClassWithMethodReference = nestedKotlinClassWithMethodReference != null
                && !toFQResource(jobRunrJob.getClass().getName()).equals(nestedKotlinClassWithMethodReference);

        if (isNestedKotlinClassWithMethodReference) {
            String location = "/" + nestedKotlinClassWithMethodReference + ".class";
            super.parse(jobRunrJob.getClass().getResourceAsStream(location));
            while (jobDetailsBuilder.getInstructions().size() > 1) {
                jobDetailsBuilder.pollFirstInstruction();
            }
        }
    }

}
