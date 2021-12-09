package org.jobrunr.scheduling;

import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.exceptions.InvalidScheduleException;
import org.jobrunr.scheduling.interval.Interval;

public class ScheduleFactory {

    private ScheduleFactory() {}

    public static Schedule getSchedule(String scheduleExpression){
        ScheduleExpressionType type = ScheduleExpressionType.getScheduleType(scheduleExpression);

        if(type.equals(ScheduleExpressionType.CRON_EXPRESSION)){
            return CronExpression.create(scheduleExpression);
        }
        else if(type.equals(ScheduleExpressionType.INTERVAL)){
            return new Interval(scheduleExpression);
        }
        else {
            throw new InvalidScheduleException("Schedule expression cannot be mapped to any type");
        }
    }
}
