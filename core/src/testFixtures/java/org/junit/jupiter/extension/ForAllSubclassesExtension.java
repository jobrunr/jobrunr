package org.junit.jupiter.extension;

import org.jobrunr.utils.PathUtils;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ForAllSubclassesExtension implements BeforeAllCallback, AfterAllCallback {

    private static AtomicInteger atomicInteger;
    private static Class annotatedTestClass;
    private static Method setUpMethod;
    private static Method tearDownMethod;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (atomicInteger != null) return;

        annotatedTestClass = findClassWithCleanupAnnotation(context);
        setUpMethod = findMethodWithAnnotation(annotatedTestClass, BeforeAllSubclasses.class);
        tearDownMethod = findMethodWithAnnotation(annotatedTestClass, AfterAllSubclasses.class);

        String classFile = annotatedTestClass.getName().replace(".", "/") + ".class";
        final List<Path> paths = PathUtils.listItems(annotatedTestClass).collect(Collectors.toList());
        final Path abstractClass = paths.stream().filter(path -> path.endsWith(classFile)).findFirst().orElseThrow(() -> new IllegalStateException());
        final String root = abstractClass.toString().replace(classFile, "");

        final int count = (int) paths.stream()
                .map(path -> path.toString().replace(root, "").replace(".class", "").replace("/", "."))
                .map(className -> ReflectionUtils.toClass(className))
                .filter(clazz -> annotatedTestClass.isAssignableFrom(clazz))
                .count();

        atomicInteger = new AtomicInteger(count - 1);

        setUpMethod.invoke(context.getRequiredTestClass());
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        final int currentCount = atomicInteger.decrementAndGet();
        if (currentCount == 0) {
            tearDownMethod.invoke(context.getRequiredTestClass());

            atomicInteger = null;
            annotatedTestClass = null;
            setUpMethod = null;
            tearDownMethod = null;
        }
    }

    private static Class findClassWithCleanupAnnotation(ExtensionContext extensionContext) {
        return findClassWithCleanupAnnotation(extensionContext.getRequiredTestClass());
    }

    private static Class findClassWithCleanupAnnotation(Class clazz) {
        if (clazz == null) {
            throw new IllegalStateException("Could not find class with CleanupAfterSubclassesExtension");
        }

        final ExtendWith declaredAnnotation = (ExtendWith) clazz.getDeclaredAnnotation(ExtendWith.class);
        if (declaredAnnotation != null && Arrays.asList(declaredAnnotation.value()).contains(ForAllSubclassesExtension.class)) {
            return clazz;
        }
        return findClassWithCleanupAnnotation(clazz.getSuperclass());
    }

    private static Method findMethodWithAnnotation(Class clazz, Class<? extends Annotation> annotation) {
        return Arrays.asList(annotatedTestClass.getDeclaredMethods())
                .stream()
                .filter(method -> method.isAnnotationPresent(annotation))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Exactly one method should be annotated with " + annotation.getSimpleName()));
    }
}
