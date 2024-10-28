package org.jobrunr.utils.annotations;

import java.lang.annotation.Documented;

/**
 * Annotation that must be used if a method is locking a job. This is added for documentation purposes and ArchUnit
 * so we know that it locks a job and hopefully prevents a possible deadlock.
 */
@Documented
public @interface LockingJob {
    String value();
}
