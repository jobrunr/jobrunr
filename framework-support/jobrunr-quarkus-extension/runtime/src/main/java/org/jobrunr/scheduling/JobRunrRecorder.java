package org.jobrunr.scheduling;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.scheduling.cron.CronExpression;

import java.time.ZoneId;
import java.util.Optional;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;
import static org.jobrunr.utils.StringUtils.substringBetween;

@Recorder
public class JobRunrRecorder {

    public void schedule(BeanContainer container, String id, JobDetails jobDetails, String cron, String zoneId) {
        JobScheduler scheduler = container.instance(JobScheduler.class);
        scheduler.scheduleRecurrently(getId(id), jobDetails, getCronExpression(cron), getZoneId(zoneId));
    }

    private String getId(String id) {
        return resolveStringValue(id);
    }

    private CronExpression getCronExpression(String cron) {
        return CronExpression.create(resolveStringValue(cron));
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
