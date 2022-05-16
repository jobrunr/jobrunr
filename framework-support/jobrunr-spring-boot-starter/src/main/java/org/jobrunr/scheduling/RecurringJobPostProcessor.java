package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.jobrunr.spring.annotations.Recurring;
import org.jobrunr.utils.StringUtils;
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

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
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
            if (hasParametersOutsideOfJobContext(method)) {
                throw new IllegalStateException("Methods annotated with " + Recurring.class.getName() + " can only have zero parameters or a single parameter of type JobContext.");
            }

            final Recurring recurringAnnotation = method.getAnnotation(Recurring.class);
            String id = getId(recurringAnnotation);
            String cron = resolveStringValue(recurringAnnotation.cron());
            String interval = resolveStringValue(recurringAnnotation.interval());

            if (StringUtils.isNullOrEmpty(cron) && StringUtils.isNullOrEmpty(interval))
                throw new IllegalArgumentException("Either cron or interval attribute is required.");
            if (StringUtils.isNotNullOrEmpty(cron) && StringUtils.isNotNullOrEmpty(interval))
                throw new IllegalArgumentException("Both cron and interval attribute provided. Only one is allowed.");

            if (Recurring.RECURRING_JOB_DISABLED.equals(cron) || Recurring.RECURRING_JOB_DISABLED.equals(interval)) {
                if (id == null) {
                    LOGGER.warn("You are trying to disable a recurring job using placeholders but did not define an id.");
                } else {
                    jobScheduler.delete(id);
                }
            } else {
                JobDetails jobDetails = getJobDetails(method);
                ZoneId zoneId = getZoneId(recurringAnnotation);
                if (isNotNullOrEmpty(cron)) {
                    jobScheduler.scheduleRecurrently(id, jobDetails, CronExpression.create(cron), zoneId);
                } else {
                    jobScheduler.scheduleRecurrently(id, jobDetails, new Interval(interval), zoneId);
                }
            }
        }

        private boolean hasParametersOutsideOfJobContext(Method method) {
            if(method.getParameterCount() == 0) return false;
            else if(method.getParameterCount() > 1) return true;
            else return !method.getParameterTypes()[0].equals(JobContext.class);
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
