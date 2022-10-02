package org.jobrunr.jobs.details;

import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.io.InputStream;
import java.lang.invoke.SerializedLambda;

import static java.util.Arrays.stream;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getJavaClassContainingLambdaAsInputStream;
import static org.jobrunr.utils.StringUtils.substringAfter;

public class JavaJobDetailsFinder extends AbstractJobDetailsFinder {

    private final JobRunrJob jobRunrJob;
    private final SerializedLambda serializedLambda;
    private final boolean isLambda;

    JavaJobDetailsFinder(JobRunrJob jobRunrJob, SerializedLambda serializedLambda, Object... params) {
        super(new JavaJobDetailsBuilder(serializedLambda, params));
        this.jobRunrJob = jobRunrJob;
        this.serializedLambda = serializedLambda;
        this.isLambda = (serializedLambda.getImplMethodName().startsWith("lambda$") || serializedLambda.getImplMethodName().contains("$lambda-") || serializedLambda.getImplMethodName().contains("$lambda$"));
        if(isLambda) {
            parse(getClassContainingLambdaAsInputStream());
        } else if(serializedLambda.getCapturedArgCount() == 1 &&
                stream(serializedLambda.getCapturedArg(0).getClass().getAnnotations())
                        .anyMatch(ann -> "kotlin.Metadata".equals(ann.annotationType().getName()))) {
            // kotlin method reference
            this.jobDetailsBuilder.setClassName(serializedLambda.getCapturedArg(0).getClass().getName());
            this.jobDetailsBuilder.setMethodName(serializedLambda.getImplMethodName().contains("$") ?
                    substringAfter(serializedLambda.getImplMethodName(), "$")
                    : serializedLambda.getImplMethodName());
        }
    }

    @Override
    protected boolean isLambdaContainingJobDetails(String name) {
        return isLambda && name.equals(serializedLambda.getImplMethodName());
    }

    @Override
    protected InputStream getClassContainingLambdaAsInputStream() {
        return getJavaClassContainingLambdaAsInputStream(jobRunrJob);
    }

}
