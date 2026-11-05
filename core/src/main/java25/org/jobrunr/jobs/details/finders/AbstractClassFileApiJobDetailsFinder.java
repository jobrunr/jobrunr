package org.jobrunr.jobs.details.finders;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.JobDetailsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeModel;

import static org.jobrunr.JobRunrException.shouldNotHappenException;

abstract class AbstractClassFileApiJobDetailsFinder {

    protected final JobDetailsBuilder jobDetailsBuilder;
    private final ClassFileApiInstructions classFileApiInstructions;

    protected AbstractClassFileApiJobDetailsFinder(JobDetailsBuilder jobDetailsBuilder) {
        this.jobDetailsBuilder = jobDetailsBuilder;
        this.classFileApiInstructions = new ClassFileApiInstructions(jobDetailsBuilder);
    }

    public JobDetails getJobDetails() {
        return jobDetailsBuilder.getJobDetails();
    }

    protected void visit(CodeModel codeModel) {
        classFileApiInstructions.visit(codeModel);
    }

    protected static ClassModel parseClass(InputStream inputStream) {
        try (InputStream classContainingLambda = inputStream) {
            return ClassFile.of().parse(classContainingLambda.readAllBytes());
        } catch (IOException e) {
            throw shouldNotHappenException(e);
        }
    }
}
