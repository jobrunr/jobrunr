package org.jobrunr.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
@Documented
public @interface Recurring {

    String id() default "";

    String cron();

    String zoneId() default "";

}