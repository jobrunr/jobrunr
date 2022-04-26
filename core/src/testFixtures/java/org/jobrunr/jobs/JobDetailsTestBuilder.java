package org.jobrunr.jobs;

import org.jobrunr.stubs.TestService;

import java.util.ArrayList;

public class JobDetailsTestBuilder {

    private boolean isCacheable;
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
                .withCacheable(true)
                .withClassName(TestService.class)
                .withMethodName("doWork")
                .withJobParameter(5);
    }

    public static JobDetailsTestBuilder systemOutPrintLnJobDetails(String message) {
        return jobDetails()
                .withCacheable(true)
                .withClassName(System.class)
                .withStaticFieldName("out")
                .withMethodName("println")
                .withJobParameter(message);
    }

    public static JobDetailsTestBuilder classThatDoesNotExistJobDetails() {
        return jobDetails()
                .withClassName("i.dont.exist.Class")
                .withMethodName("notImportant")
                .withJobParameter(5);
    }

    public static JobDetailsTestBuilder methodThatDoesNotExistJobDetails() {
        return jobDetails()
                .withClassName(TestService.class)
                .withMethodName("doWorkThatDoesNotExist")
                .withJobParameter(5);
    }

    public static JobDetailsTestBuilder jobParameterThatDoesNotExistJobDetails() {
        return jobDetails()
                .withClassName(TestService.class)
                .withMethodName("doWork")
                .withJobParameter(new JobParameter("i.dont.exist.Class", null));
    }

    private JobDetailsTestBuilder withCacheable(boolean isCacheable) {
        this.isCacheable = isCacheable;
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
        this.jobParameters.add(new JobParameter(object));
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
        final JobDetails jobDetails = new JobDetails(className, staticFieldName, methodName, jobParameters);
        jobDetails.setCacheable(isCacheable);
        return jobDetails;
    }

}