package org.jobrunr.jobs.details.finders;

import org.jobrunr.jobs.details.KotlinJobDetailsBuilder;
import org.jobrunr.jobs.details.lambdas.KotlinClassBasedLambda;
import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class KotlinAsmJobDetailsFinder extends AbstractAsmJobDetailsFinder {

    private final JobRunrJob jobRunrJob;
    private final KotlinClassBasedLambda kotlinLambda;

    private String nestedKotlinClassWithMethodReference;

    public KotlinAsmJobDetailsFinder(JobRunrJob jobRunrJob, Object... params) {
        super(new KotlinJobDetailsBuilder(jobRunrJob, params));
        this.jobRunrJob = jobRunrJob;
        this.kotlinLambda = new KotlinClassBasedLambda(jobRunrJob, jobDetailsBuilder);
        Optional<Field> kotlinFunction = kotlinLambda.getKotlinFunction();
        if (kotlinFunction.isPresent()) {
            kotlinLambda.setJobDetailsFromKotlinFunction(kotlinFunction.get());
        } else {
            parseKotlinLambdaClass();
        }
    }

    @Override
    protected boolean isLambdaContainingJobDetails(String name) {
        return kotlinLambda.matches(name);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (access == 0x1018 || access == 0x1000) {
            this.nestedKotlinClassWithMethodReference = name;
        }
    }

    private void parseKotlinLambdaClass() {
        try (InputStream inputStream = kotlinLambda.getClassContainingLambdaAsInputStream()) {
            parse(inputStream);
            parseNestedClassIfItIsAMethodReference();
        } catch (IOException e) {
            throw shouldNotHappenException(e);
        }
    }

    private void parseNestedClassIfItIsAMethodReference() {
        if (nestedKotlinClassWithMethodReference != null && kotlinLambda.isMethodReferenceNestedClass(nestedKotlinClassWithMethodReference)) {
            String location = "/" + nestedKotlinClassWithMethodReference + ".class";
            try (InputStream inputStream = jobRunrJob.getClass().getResourceAsStream(location)) {
                parse(inputStream);
                kotlinLambda.trimToSingleInstruction();
            } catch (IOException e) {
                throw shouldNotHappenException(e);
            }
        }
    }

}
