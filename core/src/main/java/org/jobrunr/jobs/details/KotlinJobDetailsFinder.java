package org.jobrunr.jobs.details;

import org.jobrunr.jobs.lambdas.JobRunrJob;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getKotlinClassContainingLambdaAsInputStream;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromField;

public class KotlinJobDetailsFinder extends AbstractJobDetailsFinder {

    private enum KotlinVersion {
        OneFour,
        OneFive
    }

    private int methodCounter = 0;
    private KotlinVersion kotlinVersion;
    private JobRunrJob jobRunrJob;

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
                        kotlinVersion = KotlinVersion.OneFour;
                    } else if (version[0] == 1 && version[1] == 5) {
                        kotlinVersion = KotlinVersion.OneFive;
                    } else {
                        throw new UnsupportedOperationException("The Kotlin version " + version[0] + "." + version[1] + " is unsupported");
                    }
                }
            }
        };
    }

    @Override
    protected boolean isLambdaContainingJobDetails(String name) {
        if (name.equals("accept") || name.equals("invoke")) {
            methodCounter++;
        }
        if (KotlinVersion.OneFive.equals(kotlinVersion)) {
            return name.equals("run") || ((name.equals("accept") || name.equals("invoke")) && methodCounter == 1);
        } else {
            return name.equals("run") || ((name.equals("accept") || name.equals("invoke")) && methodCounter == 2);
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
        if (nestedKotlinClassWithMethodReference != null) {
            String location = "/" + nestedKotlinClassWithMethodReference + ".class";
            super.parse(jobRunrJob.getClass().getResourceAsStream(location));
            while (jobDetailsBuilder.getInstructions().size() > 1) {
                jobDetailsBuilder.pollFirstInstruction();
            }
        }
    }

}
