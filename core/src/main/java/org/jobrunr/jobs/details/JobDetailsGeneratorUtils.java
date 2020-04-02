package org.jobrunr.jobs.details;

import org.jobrunr.JobRunrException;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class JobDetailsGeneratorUtils {

    private JobDetailsGeneratorUtils() {
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_TYPE_MAPPING = new HashMap<>();

    static {
        PRIMITIVE_TO_TYPE_MAPPING.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_TYPE_MAPPING.put(byte.class, Byte.class);
        PRIMITIVE_TO_TYPE_MAPPING.put(short.class, Short.class);
        PRIMITIVE_TO_TYPE_MAPPING.put(char.class, Character.class);
        PRIMITIVE_TO_TYPE_MAPPING.put(int.class, Integer.class);
        PRIMITIVE_TO_TYPE_MAPPING.put(long.class, Long.class);
        PRIMITIVE_TO_TYPE_MAPPING.put(float.class, Float.class);
        PRIMITIVE_TO_TYPE_MAPPING.put(double.class, Double.class);
    }

    public static String toFQClassName(String byteCodeName) {
        return byteCodeName.replace("/", ".");
    }

    public static String toFQResource(String byteCodeName) {
        return byteCodeName.replace(".", "/");
    }

    public static InputStream getClassContainingLambdaAsInputStream(Object lambda) {
        String name = lambda.getClass().getName();
        String location = "/" + toFQResource(name.substring(0, name.indexOf("$$"))) + ".class";
        return lambda.getClass().getResourceAsStream(location);
    }

    public static Object createObjectViaConstructor(String fqClassName, Class[] paramTypes, Object[] parameters) {
        try {
            Class clazz = Class.forName(fqClassName);
            Constructor constructor = clazz.getDeclaredConstructor(paramTypes);
            return constructor.newInstance(parameters);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public static Object createObjectViaMethod(Object objectWithMethodToInvoke, String methodName, Class[] paramTypes, Object[] parameters) {
        try {
            Class<?> clazz = objectWithMethodToInvoke.getClass();
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            return method.invoke(objectWithMethodToInvoke, parameters);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public static Object createObjectViaStaticMethod(String fqClassName, String methodName, Class[] paramTypes, Object[] parameters) {
        try {
            Class<?> clazz = Class.forName(fqClassName);
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            return method.invoke(null, parameters);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public static boolean isClassAssignableToObject(Class clazz, Object object) {
        return clazz.equals(object.getClass())
                || (clazz.isPrimitive() && PRIMITIVE_TO_TYPE_MAPPING.get(clazz).equals(object.getClass()))
                || (clazz.isPrimitive() && Boolean.TYPE.equals(clazz) && Integer.class.equals(object.getClass()));
    }

    public static Class[] findParamTypesForDescriptorAsArray(String desc) {
        return findParamTypesForDescriptor(desc).stream().toArray(Class[]::new);
    }

    public static List<Class> findParamTypesForDescriptor(String desc) {
        int beginIndex = desc.indexOf('(');
        int endIndex = desc.lastIndexOf(')');

        if ((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
            throw JobRunrException.shouldNotHappenException("Could not find the parameterTypes in the descriptor " + desc);
        }
        String x0;
        if (beginIndex == -1 && endIndex == -1) {
            x0 = desc;
        } else {
            x0 = desc.substring(beginIndex + 1, endIndex);
        }
        Pattern pattern = Pattern.compile("\\[*L[^;]+;|\\[[ZBCSIFDJ]|[ZBCSIFDJ]"); //Regex for desc \[*L[^;]+;|\[[ZBCSIFDJ]|[ZBCSIFDJ]
        Matcher matcher = pattern.matcher(x0);
        List<Class> paramTypes = new ArrayList<>();
        while (matcher.find()) {
            String paramType = matcher.group();
            if ("Z".equals(paramType)) paramTypes.add(boolean.class);
            else if ("I".equals(paramType)) paramTypes.add(int.class);
            else if ("J".equals(paramType)) paramTypes.add(long.class);
            else if ("F".equals(paramType)) paramTypes.add(float.class);
            else if ("D".equals(paramType)) paramTypes.add(double.class);
            else if ("B".equals(paramType) || "S".equals(paramType) || "C".equals(paramType))
                throw new JobRunrException("Error parsing lambda", new IllegalArgumentException("Parameters of type byte, short and char are not supported currently."));
            else if (paramType.startsWith("L")) paramTypes.add(toClass(toFQClassName(paramType.substring(1).replace(";", ""))));
            else throw JobRunrException.shouldNotHappenException("A classType was found which is not known: " + paramType);
        }
        return paramTypes;
    }
}
