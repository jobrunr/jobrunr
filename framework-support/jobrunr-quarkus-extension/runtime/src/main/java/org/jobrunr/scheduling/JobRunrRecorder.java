package org.jobrunr.scheduling;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.quarkus.annotations.Recurring;
import org.jobrunr.scheduling.cron.CronExpression;

import java.time.ZoneId;
import java.util.Optional;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;
import static org.jobrunr.utils.StringUtils.substringBetween;

@Recorder
public class JobRunrRecorder {

    private static final Logger LOGGER = Logger.getLogger(JobRunrRecorder.class);

    public void schedule(BeanContainer container, String id, JobDetails jobDetails, String cron, String zoneId) {
        JobScheduler scheduler = container.instance(JobScheduler.class);
        String jobId = getId(id);
        String cronExpression = getCronExpression(cron);
        if (Recurring.CRON_DISABLED.equals(cronExpression)) {
            if (isNullOrEmpty(jobId)) {
                LOGGER.warn("You are trying to disable a recurring job using placeholders but did not define an id.");
            } else {
                scheduler.delete(jobId);
            }
        } else {
            scheduler.scheduleRecurrently(jobId, jobDetails, CronExpression.create(cronExpression), getZoneId(zoneId));
        }
    }

    private String getId(String id) {
        return resolveStringValue(id);
    }

    private String getCronExpression(String cron) {
        return resolveStringValue(cron);
    }

    private ZoneId getZoneId(String zoneId) {
        return isNullOrEmpty(zoneId) ? ZoneId.systemDefault() : ZoneId.of(resolveStringValue(zoneId));
    }

    private String resolveStringValue(String value) {
        if (isNotNullOrEmpty(value)) {
            return Optional
                    .ofNullable(substringBetween(value, "${", "}"))
                    .map(propertyKey -> value.replace("${" + propertyKey + "}", ConfigProvider.getConfig().getValue(propertyKey, String.class)))
                    .orElse(value);
        }
        return value;
    }
}
