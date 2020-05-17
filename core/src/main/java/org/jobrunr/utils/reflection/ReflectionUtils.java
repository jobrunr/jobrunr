package org.jobrunr.utils.reflection;

import org.jobrunr.utils.StringUtils;
import org.jobrunr.utils.exceptions.Exceptions.ThrowingFunction;
import org.jobrunr.utils.reflection.autobox.Autoboxer;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class ReflectionUtils {

    private static final String rootPackageName = "org/jobrunr/";

    private ReflectionUtils() {

    }

    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static <T> Class<T> toClassFromPath(Path path) {
        final String classFile = path.toString().substring(path.toString().indexOf(rootPackageName));
        final String className = classFile.replace(".class", "").replace("/", ".");
        return toClass(className);
    }

    public static <T> Class<T> toClass(String className) {
        switch (className) {
            case "boolean":
                return cast(boolean.class);
            case "byte":
                return cast(byte.class);
            case "short":
                return cast(short.class);
            case "int":
                return cast(int.class);
            case "long":
                return cast(long.class);
            case "float":
                return cast(float.class);
            case "double":
                return cast(double.class);
            case "char":
                return cast(char.class);
            case "void":
                return cast(void.class);
            default:
                try {
                    return cast(Class.forName(className));
                } catch (ClassNotFoundException ex) {
                    throw new IllegalArgumentException("Class not found: " + className, ex);
                }
        }
    }

    public static <T> T newInstanceOrElse(String className, T orElse) {
        try {
            return newInstanceCE(toClass(className));
        } catch (Exception e) {
            return orElse;
        }
    }

    public static boolean hasDefaultNoArgConstructor(String clazzName) {
        return Stream.of(toClass(clazzName).getConstructors())
                .anyMatch((c) -> c.getParameterCount() == 0);
    }

    public static boolean hasDefaultNoArgConstructor(Class<?> clazz) {
        return Stream.of(clazz.getConstructors())
                .anyMatch((c) -> c.getParameterCount() == 0);
    }

    public static <T> T newInstance(Class<T> clazz, Map<String, String> fieldValues) {
        T t = newInstance(clazz);
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            setFieldsUsingAutoboxing(field, t, fieldValues.get(field.getName()));
        }
        return t;
    }

    public static <T> T newInstance(Class<T> clazz, Object... params) {
        try {
            return newInstanceCE(clazz, params);
        } catch (ReflectiveOperationException e) {
            throw shouldNotHappenException(e);
        }
    }

    public static <T> T newInstanceCE(Class<T> clazz, Object... params) throws ReflectiveOperationException {
        final Constructor<T> declaredConstructor = getConstructorForArgs(clazz, Stream.of(params).map(Object::getClass).toArray(Class[]::new));
        makeAccessible(declaredConstructor);
        return declaredConstructor.newInstance(params);
    }

    public static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> defaultConstructor = clazz.getDeclaredConstructor();
            makeAccessible(defaultConstructor);
            return defaultConstructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw shouldNotHappenException(e);
        }
    }

    public static Optional<Method> findMethod(Class<?> clazz, String methodName, Class[] parameterTypes) {
        return stream(clazz.getMethods())
                .filter(m -> methodName.equals(m.getName()) && Arrays.equals(m.getParameterTypes(), parameterTypes))
                .findFirst();
    }

    public static boolean objectContainsFieldOrProperty(Object object, String fieldName) {
        if (object == null) return false;
        return objectContainsField(object, fieldName) || objectContainsProperty(object, fieldName);
    }

    public static Object getValueFromFieldOrProperty(Object object, String paramName) {
        try {
            Class<?> aClass = object.getClass();
            if (objectContainsField(object, paramName)) {
                Field declaredField = aClass.getDeclaredField(paramName);
                makeAccessible(declaredField);
                return declaredField.get(object);
            } else {
                Method getter = aClass.getDeclaredMethod("get" + StringUtils.capitalize(paramName));
                makeAccessible(getter);
                return getter.invoke(object);
            }
        } catch (ReflectiveOperationException willNotHappen) {
            throw new IllegalArgumentException(String.format("Could not get value '%s' from object with class %s", paramName, object.getClass()));
        }
    }

    public static void autobox(Field field, Object object, ThrowingFunction<Class, Object> objectFunction) {
        final Class<?> type = field.getType();
        try {
            final Object value = objectFunction.apply(type);
            setFieldsUsingAutoboxing(field, object, value);
        } catch (Exception e) {
            throw shouldNotHappenException(e);
        }
    }

    public static void setFieldsUsingAutoboxing(Field field, Object object, Object value) {
        try {
            if (value == null) return;

            makeAccessible(field);
            final Class<?> type = field.getType();

            Object fieldValue = autobox(value, type);
            field.set(object, fieldValue);
        } catch (ReflectiveOperationException e) {
            throw shouldNotHappenException(e);
        }
    }

    public static <T> T autobox(Object value, Class<T> type) {
        return Autoboxer.autobox(value, type);
    }

    public static void makeAccessible(AccessibleObject accessibleObject) {
        accessibleObject.setAccessible(true);
    }

    public static NoSuchMethodException noSuchMethodException(String clazz, String methodName, Class[] parameterTypes) {
        return new NoSuchMethodException(clazz + "." + methodName + "(" + Stream.of(parameterTypes).map(Class::getName).collect(joining(",")) + ")");
    }

    private static <T> Constructor<T> getConstructorForArgs(Class<T> clazz, Class[] args) throws NoSuchMethodException {
        Constructor<?>[] constructors = clazz.getConstructors();

        for (Constructor<?> constructor : constructors) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length == args.length) {
                boolean argumentsMatch = true;
                for (int i = 0; i < args.length; i++) {
                    if (!types[i].isAssignableFrom(args[i])) {
                        argumentsMatch = false;
                        break;
                    }
                }

                if (argumentsMatch) return cast(constructor);
            }
        }
        throw noSuchMethodException(clazz.getName(), "<init>", args);
    }

    private static <T> Class<T> cast(Class<?> aClass) {
        return (Class<T>) aClass;
    }

    public static <T> T cast(Object anObject) {
        return (T) anObject;
    }

    private static boolean objectContainsField(Object object, String fieldName) {
        return stream(object.getClass().getDeclaredFields()).anyMatch(f -> f.getName().equals(fieldName));
    }

    private static boolean objectContainsProperty(Object object, String fieldName) {
        return stream(object.getClass().getDeclaredMethods()).anyMatch(m -> m.getName().equalsIgnoreCase("get" + fieldName));
    }
}
