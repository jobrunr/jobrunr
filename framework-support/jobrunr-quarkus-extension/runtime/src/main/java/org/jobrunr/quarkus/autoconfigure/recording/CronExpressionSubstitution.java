package org.jobrunr.quarkus.autoconfigure.recording;

import io.quarkus.runtime.ObjectSubstitution;
import org.jobrunr.scheduling.cron.CronExpression;

public class CronExpressionSubstitution implements ObjectSubstitution<CronExpression, String> {

    @Override
    public String serialize(CronExpression cronExpression) {
        return cronExpression.getExpression();
    }

    @Override
    public CronExpression deserialize(String cron) {
        return CronExpression.create(cron);
    }
}
