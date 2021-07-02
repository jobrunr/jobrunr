package org.jobrunr.jobs.details;

import org.jobrunr.JobRunrException;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jobrunr.utils.reflection.ReflectionUtils.getField;
import static org.jobrunr.utils.reflection.ReflectionUtils.getMethod;
import static org.jobrunr.utils.reflection.ReflectionUtils.loadClass;
import static org.jobrunr.utils.reflection.ReflectionUtils.makeAccessible;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class JobDetailsGeneratorUtils {

    private JobDetailsGeneratorUtils() {
    }

    public static String toFQClassName(String byteCodeName) {
        return byteCodeName.replace("/", ".");
    }

    public static String toFQResource(String byteCodeName) {
        return byteCodeName.replace(".", "/");
    }

    public static InputStream getJavaClassContainingLambdaAsInputStream(Object lambda) {
        String location = getClassLocationOfLambda(lambda);
        return lambda.getClass().getResourceAsStream(location);
    }

    public static InputStream getKotlinClassContainingLambdaAsInputStream(Object lambda) {
        String name = lambda.getClass().getName();
        String location = "/" + toFQResource(name) + ".class";
        return lambda.getClass().getResourceAsStream(location);
    }

    public static String getClassLocationOfLambda(Object lambda) {
        String name = lambda.getClass().getName();
        return "/" + toFQResource(name.substring(0, name.indexOf("$$"))) + ".class";
    }

    public static Object createObjectViaConstructor(String fqClassName, Class<?>[] paramTypes, Object[] parameters) {
        try {
            Class<?> clazz = loadClass(fqClassName);
            Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
            return constructor.newInstance(parameters);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public static Object createObjectViaMethod(Object objectWithMethodToInvoke, String methodName, Class<?>[] paramTypes, Object[] parameters) {
        try {
            Class<?> clazz = objectWithMethodToInvoke.getClass();
            if (clazz.isSynthetic())
                throw new IllegalArgumentException("You are passing another (nested) Java 8 lambda to JobRunr - this is not supported. Try to convert your lambda to a class or a method.");
            Method method = getMethod(clazz, methodName, paramTypes);
            makeAccessible(method);
            return method.invoke(objectWithMethodToInvoke, parameters);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public static Object createObjectViaStaticMethod(String fqClassName, String methodName, Class<?>[] paramTypes, Object[] parameters) {
        try {
            Class<?> clazz = loadClass(fqClassName);
            Method method = getMethod(clazz, methodName, paramTypes);
            return method.invoke(null, parameters);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public static Object getObjectViaStaticField(String fqClassName, String fieldName) {
        try {
            Class<?> clazz = loadClass(fqClassName);
            Field field = getField(clazz, fieldName);
            makeAccessible(field);
            return field.get(null);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public static Object getObjectViaField(Object object, String fieldName) {
        try {
            Class<?> clazz = object.getClass();
            Field field = getField(clazz, fieldName);
            makeAccessible(field);
            return field.get(object);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public static Class<?>[] findParamTypesFromDescriptorAsArray(String desc) {
        return findParamTypesFromDescriptor(desc).toArray(new Class[0]);
    }

    public static List<Class<?>> findParamTypesFromDescriptor(String desc) {
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
        List<Class<?>> paramTypes = new ArrayList<>();
        while (matcher.find()) {
            String paramType = matcher.group();
            Class<?> clazzToAdd = getClassToAdd(paramType);
            paramTypes.add(clazzToAdd);
        }
        return paramTypes;
    }

    private static Class<?> getClassToAdd(String paramType) {
        if ("Z".equals(paramType)) return boolean.class;
        else if ("I".equals(paramType)) return int.class;
        else if ("J".equals(paramType)) return long.class;
        else if ("F".equals(paramType)) return float.class;
        else if ("D".equals(paramType)) return double.class;
        else if ("B".equals(paramType) || "S".equals(paramType) || "C".equals(paramType))
            throw new JobRunrException("Error parsing lambda", new IllegalArgumentException("Parameters of type byte, short and char are not supported currently."));
        else if (paramType.startsWith("L")) return toClass(toFQClassName(paramType.substring(1).replace(";", "")));
        else if (paramType.startsWith("[")) return toClass(toFQClassName(paramType));
        else throw JobRunrException.shouldNotHappenException("A classType was found which is not known: " + paramType);
    }
}
