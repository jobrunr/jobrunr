package org.jobrunr.jobs.details.finders;

import org.jobrunr.jobs.details.lambdas.SerializedJobLambda;

import java.io.IOException;
import java.io.InputStream;

import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class SerializedLambdaAsmJobDetailsFinder extends AbstractAsmJobDetailsFinder {

    private final SerializedJobLambda lambda;

    public SerializedLambdaAsmJobDetailsFinder(SerializedJobLambda lambda, Object... params) {
        super(lambda.createJobDetailsBuilder(params));
        this.lambda = lambda;
        if (lambda.isLambdaBody()) {
            try (InputStream classContainingLambdaInputStream = lambda.getClassContainingLambdaAsInputStream()) {
                parse(classContainingLambdaInputStream);
            } catch (IOException e) {
                throw shouldNotHappenException(e);
            }
        }
    }

    @Override
    protected boolean isLambdaContainingJobDetails(String name) {
        return lambda.matches(name);
    }

}
