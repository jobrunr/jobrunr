package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.spring.annotations.Recurring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.util.ArrayList;

import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

public class RecurringJobPostProcessor implements BeanPostProcessor, EmbeddedValueResolverAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecurringJobPostProcessor.class);

    private final JobScheduler jobScheduler;
    private StringValueResolver embeddedValueResolver;
    private RecurringJobFinderMethodCallback recurringJobFinderMethodCallback;

    public RecurringJobPostProcessor(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithMethods(bean.getClass(), recurringJobFinderMethodCallback);
        return bean;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    @Override
    public void afterPropertiesSet() {
        this.recurringJobFinderMethodCallback = new RecurringJobFinderMethodCallback(jobScheduler, embeddedValueResolver);
    }

    private static class RecurringJobFinderMethodCallback implements ReflectionUtils.MethodCallback {

        private final JobScheduler jobScheduler;
        private final StringValueResolver embeddedValueResolver;

        public RecurringJobFinderMethodCallback(JobScheduler jobScheduler, StringValueResolver resolver) {
            this.jobScheduler = jobScheduler;
            this.embeddedValueResolver = resolver;
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
            String cron = resolveStringValue(recurringAnnotation.cron());

            if (Recurring.CRON_DISABLED.equals(cron)) {
                if (id == null) {
                    LOGGER.warn("You are trying to disable a recurring job using placeholders but did not define an id.");
                } else {
                    jobScheduler.delete(id);
                }
            } else {
                JobDetails jobDetails = getJobDetails(method);
                CronExpression cronExpression = CronExpression.create(cron);
                ZoneId zoneId = getZoneId(recurringAnnotation);

                jobScheduler.scheduleRecurrently(id, jobDetails, cronExpression, zoneId);
            }
        }

        private String getId(Recurring recurringAnnotation) {
            String id = resolveStringValue(recurringAnnotation.id());
            return isNullOrEmpty(id) ? null : id;
        }

        private JobDetails getJobDetails(Method method) {
            final JobDetails jobDetails = new JobDetails(method.getDeclaringClass().getName(), null, method.getName(), new ArrayList<>());
            jobDetails.setCacheable(true);
            return jobDetails;
        }

        private ZoneId getZoneId(Recurring recurringAnnotation) {
            String zoneId = resolveStringValue(recurringAnnotation.zoneId());
            return isNullOrEmpty(zoneId) ? ZoneId.systemDefault() : ZoneId.of(zoneId);
        }

        private String resolveStringValue(String value) {
            if (embeddedValueResolver != null && value != null) {
                return embeddedValueResolver.resolveStringValue(value);
            }
            return value;
        }
    }
}
