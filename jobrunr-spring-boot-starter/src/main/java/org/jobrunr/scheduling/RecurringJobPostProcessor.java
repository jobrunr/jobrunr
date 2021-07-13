package org.jobrunr.scheduling;

import org.jobrunr.annotations.Recurring;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.scheduling.cron.CronExpression;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.util.ArrayList;

import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

public class RecurringJobPostProcessor implements BeanPostProcessor {

    private final RecurringJobFinderMethodCallback recurringJobFinderMethodCallback;

    public RecurringJobPostProcessor(JobScheduler jobScheduler) {
        this.recurringJobFinderMethodCallback = new RecurringJobFinderMethodCallback(jobScheduler);

    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithMethods(bean.getClass(), recurringJobFinderMethodCallback);
        return bean;
    }

    private static class RecurringJobFinderMethodCallback implements ReflectionUtils.MethodCallback {

        private final JobScheduler jobScheduler;

        public RecurringJobFinderMethodCallback(JobScheduler jobScheduler) {
            this.jobScheduler = jobScheduler;
        }

        @Override
        public void doWith(Method method) throws IllegalArgumentException {
            if (!method.isAnnotationPresent(Recurring.class)) {
                return;
            }
            if (method.getParameterCount() > 0) {
                throw new IllegalStateException("Methods annotated with " + Recurring.class.getName() + " can not have parameters.");
            }

            final Recurring recurringAnnotation = method.getAnnotation(Recurring.class);
            String id = getId(recurringAnnotation);
            JobDetails jobDetails = getJobDetails(method);
            CronExpression cronExpression = getCronExpression(recurringAnnotation);
            ZoneId zoneId = getZoneId(recurringAnnotation);

            jobScheduler.scheduleRecurrently(id, jobDetails, cronExpression, zoneId);
        }

        private String getId(Recurring recurringAnnotation) {
            return isNullOrEmpty(recurringAnnotation.id()) ? null : recurringAnnotation.id();
        }

        private JobDetails getJobDetails(Method method) {
            return new JobDetails(
                    method.getDeclaringClass().getName(),
                    null,
                    method.getName(),
                    new ArrayList<>()
            );
        }

        private CronExpression getCronExpression(Recurring recurringAnnotation) {
            return CronExpression.create(recurringAnnotation.cron());
        }

        private ZoneId getZoneId(Recurring recurringAnnotation) {
            return isNullOrEmpty(recurringAnnotation.zoneId()) ? ZoneId.systemDefault() : ZoneId.of(recurringAnnotation.zoneId());
        }
    }
}
