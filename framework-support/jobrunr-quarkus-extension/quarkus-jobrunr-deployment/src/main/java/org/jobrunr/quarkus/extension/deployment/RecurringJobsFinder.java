package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobRunrRecurringJobRecorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecurringJobsFinder {

    private final RecorderContext recorderContext;
    private final CombinedIndexBuildItem index;
    private final BeanContainerBuildItem beanContainer;
    private final JobRunrRecurringJobRecorder recorder;

    public RecurringJobsFinder(RecorderContext recorderContext, CombinedIndexBuildItem index, BeanContainerBuildItem beanContainer, JobRunrRecurringJobRecorder recorder) throws NoSuchMethodException {
        this.recorderContext = recorderContext;
        this.index = index;
        this.beanContainer = beanContainer;
        this.recorder = recorder;

        registerNonDefaultJobDetailsConstructor();
        registerNonDefaultJobParameterConstructor();
    }

    public void findRecurringJobsAndScheduleThem() {
        for (AnnotationInstance recurringJobAnnotation : index.getIndex().getAnnotations(DotName.createSimple(Recurring.class.getName()))) {
            AnnotationTarget annotationTarget = recurringJobAnnotation.target();
            if (AnnotationTarget.Kind.METHOD.equals(annotationTarget.kind())) {
                final String id = getId(recurringJobAnnotation);
                final String cron = getCron(recurringJobAnnotation);
                final String interval = getInterval(recurringJobAnnotation);
                final JobDetails jobDetails = getJobDetails(recurringJobAnnotation);
                final String zoneId = getZoneId(recurringJobAnnotation);
                recorder.schedule(beanContainer.getValue(), id, cron, interval, zoneId, jobDetails.getClassName(), jobDetails.getMethodName(), jobDetails.getJobParameters());
            }
        }
    }

    private void registerNonDefaultJobDetailsConstructor() throws NoSuchMethodException {
        recorderContext.registerNonDefaultConstructor(JobDetails.class.getDeclaredConstructor(String.class, String.class, String.class, List.class), jobDetails -> Arrays.asList(
                jobDetails.getClassName(),
                jobDetails.getStaticFieldName(),
                jobDetails.getMethodName(),
                jobDetails.getJobParameters()
        ));
    }

    private void registerNonDefaultJobParameterConstructor() throws NoSuchMethodException {
        recorderContext.registerNonDefaultConstructor(JobParameter.class.getDeclaredConstructor(String.class, String.class, Object.class), jobParameter -> Arrays.asList(
                jobParameter.getClassName(),
                jobParameter.getActualClassName(),
                jobParameter.getObject()
        ));
    }

    private String getId(AnnotationInstance recurringJobAnnotation) {
        if (recurringJobAnnotation.value("id") != null) {
            return recurringJobAnnotation.value("id").asString();
        }
        return null;
    }

    private String getCron(AnnotationInstance recurringJobAnnotation) {
        if (recurringJobAnnotation.value("cron") != null) {
            return recurringJobAnnotation.value("cron").asString();
        }
        return null;
    }

    private String getInterval(AnnotationInstance recurringJobAnnotation) {
        if (recurringJobAnnotation.value("interval") != null) {
            return recurringJobAnnotation.value("interval").asString();
        }
        return null;
    }

    private JobDetails getJobDetails(AnnotationInstance recurringJobAnnotation) {
        final MethodInfo methodInfo = recurringJobAnnotation.target().asMethod();
        if (hasParametersOutsideOfJobContext(methodInfo)) {
            throw new IllegalStateException("Methods annotated with " + Recurring.class.getName() + " can only have zero parameters or a single parameter of type JobContext.");
        }
        List<JobParameter> jobParameters = new ArrayList<>();
        if (methodInfo.parameters().size() == 1 && methodInfo.parameterType(0).name().equals(DotName.createSimple(JobContext.class.getName()))) {
            jobParameters.add(JobParameter.JobContext);
        }
        final JobDetails jobDetails = new JobDetails(methodInfo.declaringClass().name().toString(), null, methodInfo.name(), jobParameters);
        jobDetails.setCacheable(true);
        return jobDetails;
    }

    private boolean hasParametersOutsideOfJobContext(MethodInfo method) {
        if (method.parameters().isEmpty()) return false;
        else if (method.parameters().size() > 1) return true;
        else return !method.parameterType(0).name().equals(DotName.createSimple(JobContext.class.getName()));
    }

    private String getZoneId(AnnotationInstance recurringJobAnnotation) {
        if (recurringJobAnnotation.value("zoneId") != null) {
            return recurringJobAnnotation.value("zoneId").asString();
        }
        return null;
    }
}
