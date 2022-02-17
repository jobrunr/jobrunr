package org.jobrunr.utils.mapper;

import static java.util.Arrays.stream;

public class JsonMapperUtils {

    private JsonMapperUtils() {
    }

    public static String getActualClassName(String methodClassName, String actualClassName) {
        return getActualClassName(methodClassName, actualClassName, "java.", "sun.", "com.sun.");
    }

    public static String getActualClassName(String methodClassName, String actualClassName, String... classNamesThatShouldReturnTheMethodClassName) {
        if (actualClassName == null || stream(classNamesThatShouldReturnTheMethodClassName).anyMatch(actualClassName::startsWith))
            return methodClassName;
        return actualClassName;
    }

    public static final class Json {
        private Json() {
        }

        public static final String FIELD_CACHEABLE = "cacheable";
        public static final String FIELD_CLASS_NAME = "className";
        public static final String FIELD_ACTUAL_CLASS_NAME = "actualClassName";
        public static final String FIELD_STATIC_FIELD_NAME = "staticFieldName";
        public static final String FIELD_METHOD_NAME = "methodName";
        public static final String FIELD_JOB_PARAMETERS = "jobParameters";
    }
}
