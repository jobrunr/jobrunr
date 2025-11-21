package org.jobrunr.utils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;

/**
 * Denotes that this constructor is part of the builder pattern.
 */
@Documented
@Target(value = {CONSTRUCTOR})
public @interface Builder {
}
