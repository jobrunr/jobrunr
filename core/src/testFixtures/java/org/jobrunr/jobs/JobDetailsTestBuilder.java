package org.jobrunr.jobs;

import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.stubs.TestService;

import java.util.ArrayList;

public class JobDetailsTestBuilder {

    private String lambdaType;
    private String className;
    private String staticFieldName;
    private String methodName;
    private ArrayList<JobParameter> jobParameters = new ArrayList<>();

    private JobDetailsTestBuilder() {
    }

    public static JobDetailsTestBuilder jobDetails() {
        return new JobDetailsTestBuilder();
    }

    public static JobDetailsTestBuilder defaultJobDetails() {
        return jobDetails()
                .withLambdaType(JobLambda.class)
                .withClassName(TestService.class)
                .withMethodName("doWork")
                .withJobParameter(5);
    }

    public static JobDetailsTestBuilder systemOutPrintLnJobDetails(String message) {
        return jobDetails()
                .withLambdaType(JobLambda.class)
                .withClassName(System.class)
                .withStaticFieldName("out")
                .withMethodName("println")
                .withJobParameter(message);
    }

    public JobDetailsTestBuilder withLambdaType(Class lambdaType) {
        this.lambdaType = lambdaType.getName();
        return this;
    }

    public JobDetailsTestBuilder withLambdaType(String lambdaType) {
        this.lambdaType = lambdaType;
        return this;
    }

    public JobDetailsTestBuilder withClassName(Class clazz) {
        this.className = clazz.getName();
        return this;
    }

    public JobDetailsTestBuilder withClassName(String className) {
        this.className = className;
        return this;
    }

    public JobDetailsTestBuilder withStaticFieldName(String staticFieldName) {
        this.staticFieldName = staticFieldName;
        return this;
    }

    public JobDetailsTestBuilder withMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public JobDetailsTestBuilder withJobParameter(Object object) {
        this.jobParameters.add(new JobParameter(object.getClass(), object));
        return this;
    }

    public JobDetailsTestBuilder withJobParameter(JobParameter jobParameter) {
        this.jobParameters.add(jobParameter);
        return this;
    }

    public JobDetailsTestBuilder withJobParameters(ArrayList<JobParameter> jobParameters) {
        this.jobParameters = jobParameters;
        return this;
    }

    public JobDetails build() {
        return new JobDetails(lambdaType, className, staticFieldName, methodName, jobParameters);
    }

}