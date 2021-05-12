package org.jobrunr.utils.mapper;

public class JsonMapperUtils {

    private JsonMapperUtils() {
    }

    public static String getActualClassName(String methodClassName, String actualClassName) {
        if (actualClassName == null || actualClassName.startsWith("java.") || actualClassName.startsWith("sun.") || actualClassName.startsWith("com.sun"))
            return methodClassName;
        return actualClassName;
    }

    public static final class Json {
        private Json() {
        }

        public static final String FIELD_CLASS_NAME = "className";
        public static final String FIELD_ACTUAL_CLASS_NAME = "actualClassName";
        public static final String FIELD_STATIC_FIELD_NAME = "staticFieldName";
        public static final String FIELD_METHOD_NAME = "methodName";
        public static final String FIELD_JOB_PARAMETERS = "jobParameters";
    }
}
