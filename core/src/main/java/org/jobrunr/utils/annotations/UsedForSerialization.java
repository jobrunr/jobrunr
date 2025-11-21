package org.jobrunr.utils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;

/**
 * Denotes that this constructor is used for serialization
 */
@Documented
@Target(value = {CONSTRUCTOR})
public @interface UsedForSerialization {
}
