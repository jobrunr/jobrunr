package org.jobrunr.utils.mapper;

public class JsonMapperUtils {

    private JsonMapperUtils() {

    }

    public static String getActualClassName(String methodClassName, String actualClassName) {
        if (actualClassName == null || actualClassName.startsWith("java.") || actualClassName.startsWith("sun.") || actualClassName.startsWith("com.sun"))
            return methodClassName;
        return actualClassName;
    }
}
