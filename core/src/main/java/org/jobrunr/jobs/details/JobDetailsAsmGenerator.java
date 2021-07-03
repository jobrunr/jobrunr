package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.lambdas.*;

import java.lang.annotation.Annotation;

import static java.util.Arrays.stream;
import static org.jobrunr.jobs.details.SerializedLambdaConverter.toSerializedLambda;

public class JobDetailsAsmGenerator implements JobDetailsGenerator {

    @Override
    public JobDetails toJobDetails(JobLambda lambda) {
        if (isKotlinLambda(lambda)) {
            return new KotlinJobDetailsFinder(lambda).getJobDetails();
        } else {
            return new JavaJobDetailsFinder(lambda, toSerializedLambda(lambda)).getJobDetails();
        }
    }

    @Override
    public JobDetails toJobDetails(IocJobLambda lambda) {
        if (isKotlinLambda(lambda)) {
            return new KotlinJobDetailsFinder(lambda, new Object()).getJobDetails();
        } else {
            return new JavaJobDetailsFinder(lambda, toSerializedLambda(lambda)).getJobDetails();
        }
    }

    @Override
    public <T> JobDetails toJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda) {
        if (isKotlinLambda(lambda)) {
            return new KotlinJobDetailsFinder(lambda, itemFromStream).getJobDetails();
        } else {
            return new JavaJobDetailsFinder(lambda, toSerializedLambda(lambda), itemFromStream).getJobDetails();
        }
    }

    @Override
    public <S, T> JobDetails toJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda) {
        if (isKotlinLambda(lambda)) {
            // why new Object(): it represents the item injected when we run the IocJobLambdaFromStream function
            return new KotlinJobDetailsFinder(lambda, new Object(), itemFromStream).getJobDetails();
        } else {
            // why null: it represents the item injected when we run the IocJobLambdaFromStream function
            return new JavaJobDetailsFinder(lambda, toSerializedLambda(lambda), null, itemFromStream).getJobDetails();
        }
    }

    private <T extends JobRunrJob> boolean isKotlinLambda(T lambda) {
        return stream(lambda.getClass().getAnnotations()).map(Annotation::annotationType).anyMatch(annotationType -> annotationType.getName().equals("kotlin.Metadata"));
    }
}
