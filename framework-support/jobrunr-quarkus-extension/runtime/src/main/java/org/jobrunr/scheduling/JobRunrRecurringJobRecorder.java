package org.jobrunr.scheduling;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.common.expression.Expression;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.quarkus.annotations.Recurring;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.jobrunr.utils.StringUtils;

import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

@Recorder
public class JobRunrRecurringJobRecorder {

    private static final Logger LOGGER = Logger.getLogger(JobRunrRecurringJobRecorder.class);

    public void schedule(BeanContainer container, String id, JobDetails jobDetails, String cron, String interval, String zoneId) {
        JobScheduler scheduler = container.instance(JobScheduler.class);
        String jobId = getId(id);
        String optionalCronExpression = getCronExpression(cron);
        String optionalInterval = getInterval(interval);

        if (StringUtils.isNullOrEmpty(cron) && StringUtils.isNullOrEmpty(optionalInterval))
            throw new IllegalArgumentException("Either cron or interval attribute is required.");
        if (StringUtils.isNotNullOrEmpty(cron) && StringUtils.isNotNullOrEmpty(optionalInterval))
            throw new IllegalArgumentException("Both cron and interval attribute provided. Only one is allowed.");

        if (Recurring.RECURRING_JOB_DISABLED.equals(optionalCronExpression) || Recurring.RECURRING_JOB_DISABLED.equals(optionalInterval)) {
            if (isNullOrEmpty(jobId)) {
                LOGGER.warn("You are trying to disable a recurring job using placeholders but did not define an id.");
            } else {
                scheduler.delete(jobId);
            }
        } else {
            if (isNotNullOrEmpty(optionalCronExpression)) {
                scheduler.scheduleRecurrently(id, jobDetails, CronExpression.create(optionalCronExpression), getZoneId(zoneId));
            } else {
                scheduler.scheduleRecurrently(id, jobDetails, new Interval(optionalInterval), getZoneId(zoneId));
            }
        }
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
            final String expanded = expression.evaluate((resolveContext, stringBuilder) -> {
                final Optional<String> resolve = config.getOptionalValue(resolveContext.getKey(), String.class);
                if (resolve.isPresent()) {
                    stringBuilder.append(resolve.get());
                } else if (resolveContext.hasDefault()) {
                    resolveContext.expandDefault();
                } else {
                    throw new NoSuchElementException(String.format("Could not expand value %s in property %s", resolveContext.getKey(), expr));
                }
            });
            return expanded;
        }
        return expr;
    }
}
