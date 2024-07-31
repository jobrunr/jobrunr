package org.jobrunr.scheduling;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.common.expression.Expression;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.utils.StringUtils;

import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

@Recorder
public class JobRunrRecurringJobRecorder {

    private static final Logger LOGGER = Logger.getLogger(JobRunrRecurringJobRecorder.class);

    public void schedule(BeanContainer container, String id, String cron, String interval, String zoneId, String className, String methodName, List<JobParameter> parameterList) {
        JobScheduler scheduler = container.beanInstance(JobScheduler.class);
        String jobId = getId(id);
        String optionalCronExpression = getCronExpression(cron);
        String optionalInterval = getInterval(interval);

        String scheduleExpression = getScheduleExpression(optionalCronExpression, optionalInterval);

        if (Recurring.RECURRING_JOB_DISABLED.equals(scheduleExpression)) {
            if (isNullOrEmpty(jobId)) {
                LOGGER.warn("You are trying to disable a recurring job using placeholders but did not define an id.");
            } else {
                scheduler.deleteRecurringJob(jobId);
            }
        } else {
            JobDetails jobDetails = new JobDetails(className, null, methodName, parameterList);
            jobDetails.setCacheable(true);
            scheduler.scheduleRecurrently(id, jobDetails, ScheduleExpressionType.getSchedule(scheduleExpression), getZoneId(zoneId));
        }
    }

    private static String getScheduleExpression(String cron, String interval) {
        List<String> validScheduleExpressions = Stream.of(cron, interval).filter(StringUtils::isNotNullOrEmpty).toList();
        int count = validScheduleExpressions.size();
        if (count == 0) throw new IllegalArgumentException("Either cron or interval attribute is required.");
        if (count > 1) throw new IllegalArgumentException("Both cron and interval attribute provided. Only one is allowed.");
        return validScheduleExpressions.get(0);
    }

    private static String getId(String id) {
        return resolveStringValue(id);
    }

    private static String getCronExpression(String cron) {
        return resolveStringValue(cron);
    }

    private static String getInterval(String interval) {
        return resolveStringValue(interval);
    }

    private static ZoneId getZoneId(String zoneId) {
        return isNullOrEmpty(zoneId) ? ZoneId.systemDefault() : ZoneId.of(resolveStringValue(zoneId));
    }

    private static String resolveStringValue(String expr) {
        if (isNotNullOrEmpty(expr)) {
            final Config config = ConfigProvider.getConfig();
            final Expression expression = Expression.compile(expr);
            return expression.evaluate((resolveContext, stringBuilder) -> {
                final Optional<String> resolve = config.getOptionalValue(resolveContext.getKey(), String.class);
                if (resolve.isPresent()) {
                    stringBuilder.append(resolve.get());
                } else if (resolveContext.hasDefault()) {
                    resolveContext.expandDefault();
                } else {
                    throw new NoSuchElementException(String.format("Could not expand value %s in property %s", resolveContext.getKey(), expr));
                }
            });

        }
        return expr;
    }
}
