package org.jobrunr.utils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Documents why a class, field or method is visible
 */

@Retention(value = CLASS)
@Target(value = {ANNOTATION_TYPE, CONSTRUCTOR, FIELD, METHOD, TYPE})
@Documented
public @interface VisibleFor {

    String value();

}
