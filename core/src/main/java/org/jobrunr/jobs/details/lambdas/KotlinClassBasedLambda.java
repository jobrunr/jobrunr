package org.jobrunr.jobs.details.lambdas;

import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jobrunr.jobs.lambdas.JobRunrJob;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getKotlinClassContainingLambdaAsInputStream;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQResource;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromField;

public final class KotlinClassBasedLambda {

    private static final String INVOKE = "invoke";
    private static final String ACCEPT = "accept";
    private static final String RUN = "run";

    private final JobRunrJob jobRunrJob;
    private final JobDetailsBuilder jobDetailsBuilder;
    private int methodCounter;
    private KotlinVersion kotlinVersion;

    public KotlinClassBasedLambda(JobRunrJob jobRunrJob, JobDetailsBuilder jobDetailsBuilder) {
        this.jobDetailsBuilder = jobDetailsBuilder;
        this.jobRunrJob = jobRunrJob;
    }

    public boolean matches(String methodName) {
        if (methodName.equals(ACCEPT) || methodName.equals(INVOKE)) {
            methodCounter++;
        }
        if (KotlinVersion.ONE_FOUR.equals(kotlinVersion)) {
            return methodName.equals(RUN) || ((methodName.equals(ACCEPT) || methodName.equals(INVOKE)) && methodCounter == 2);
        } else {
            return methodName.equals(RUN) || ((methodName.equals(ACCEPT) || methodName.equals(INVOKE)) && methodCounter == 1);
        }
    }

    public InputStream getClassContainingLambdaAsInputStream() {
        return getKotlinClassContainingLambdaAsInputStream(jobRunrJob);
    }

    public Optional<Field> getKotlinFunction() {
        return ReflectionUtils.findField(jobRunrJob.getClass(), "function");
    }

    public void setJobDetailsFromKotlinFunction(Field field) {
        Object function = getValueFromField(field, jobRunrJob);
        Field receiver = ReflectionUtils.getField(function.getClass(), "receiver");
        Field name = ReflectionUtils.getField(function.getClass(), "name");
        Class<?> receiverClass = getValueFromField(receiver, function).getClass();
        String methodName = cast(getValueFromField(name, function));
        jobDetailsBuilder.setClassName(receiverClass.getName());
        jobDetailsBuilder.setMethodName(methodName);
    }

    public void setKotlinVersion(int[] version) {
        this.kotlinVersion = KotlinVersion.from(version);
    }

    public boolean isMethodReferenceNestedClass(String internalName) {
        return !toFQResource(jobRunrJob.getClass().getName()).equals(internalName);
    }

    public void trimToSingleInstruction() {
        while (jobDetailsBuilder.getInstructions().size() > 1) {
            jobDetailsBuilder.pollFirstInstruction();
        }
    }

    enum KotlinVersion {
        ONE_FOUR,
        ONE_FIVE,
        ONE_SIX_OR_HIGHER;

        static KotlinVersion from(int[] version) {
            if (version[0] == 1 && version[1] == 4) return ONE_FOUR;
            if (version[0] == 1 && version[1] == 5) return ONE_FIVE;
            if (version[0] > 1 || version[1] >= 6) return ONE_SIX_OR_HIGHER;
            throw new UnsupportedOperationException("The Kotlin version " + version[0] + "." + version[1] + " is unsupported");
        }
    }
}
