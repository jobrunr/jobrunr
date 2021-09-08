package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.quarkus.annotations.Recurring;
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
    }

    public void findRecurringJobsAndScheduleThem() {
        for (AnnotationInstance recurringJobAnnotation : index.getIndex().getAnnotations(DotName.createSimple(Recurring.class.getName()))) {
            AnnotationTarget annotationTarget = recurringJobAnnotation.target();
            if (AnnotationTarget.Kind.METHOD.equals(annotationTarget.kind())) {
                final String id = getId(recurringJobAnnotation);
                final String cron = getCron(recurringJobAnnotation);
                final JobDetails jobDetails = getJobDetails(recurringJobAnnotation);
                final String zoneId = getZoneId(recurringJobAnnotation);
                recorder.schedule(beanContainer.getValue(), id, jobDetails, cron, zoneId);
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

    private String getId(AnnotationInstance recurringJobAnnotation) {
        if (recurringJobAnnotation.value("id") != null) {
            return recurringJobAnnotation.value("id").asString();
        }
        return null;
    }

    private String getCron(AnnotationInstance recurringJobAnnotation) {
        return recurringJobAnnotation.value("cron").asString();
    }

    private JobDetails getJobDetails(AnnotationInstance recurringJobAnnotation) {
        final MethodInfo methodInfo = recurringJobAnnotation.target().asMethod();
        if (!methodInfo.parameters().isEmpty()) {
            throw new IllegalStateException("Methods annotated with " + Recurring.class.getName() + " can not have parameters.");
        }
        final JobDetails jobDetails = new JobDetails(methodInfo.declaringClass().name().toString(), null, methodInfo.name(), new ArrayList<>());
        jobDetails.setCacheable(true);
        return jobDetails;
    }

    private String getZoneId(AnnotationInstance recurringJobAnnotation) {
        if (recurringJobAnnotation.value("zoneId") != null) {
            return recurringJobAnnotation.value("zoneId").asString();
        }
        return null;
    }
}
