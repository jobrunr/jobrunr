package org.junit.jupiter.extension;

import org.jobrunr.utils.reflection.ReflectionUtils;
import org.jobrunr.utils.resources.ClassPathResourceProvider;
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

import static java.util.stream.Collectors.toList;

public class ForAllSubclassesExtension implements BeforeAllCallback, AfterAllCallback {

    private static AtomicInteger atomicInteger;
    private static Class annotatedTestClass;
    private static Method setUpMethod;
    private static Method tearDownMethod;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (atomicInteger != null) return;

        annotatedTestClass = findClassWithForAllSubclassesAnnotation(context);
        setUpMethod = findMethodWithAnnotation(annotatedTestClass, BeforeAllSubclasses.class);
        tearDownMethod = findMethodWithAnnotation(annotatedTestClass, AfterAllSubclasses.class);

        try(ClassPathResourceProvider resourceProvider = new ClassPathResourceProvider()) {
            final List<Path> paths = resourceProvider.listAllChildrenOnClasspath(annotatedTestClass).collect(toList());
            final int count = (int) paths.stream()
                    .filter(path -> path.toString().endsWith(".class"))
                    .map(ReflectionUtils::toClassFromPath)
                    .filter(annotatedTestClass::isAssignableFrom)
                    .count();
            atomicInteger = new AtomicInteger(count - 1);

            setUpMethod.invoke(context.getRequiredTestClass());
            System.err.println("Invoking setup method for " + annotatedTestClass.getName());
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        final int currentCount = atomicInteger.decrementAndGet();
        if (currentCount == 0) {
            tearDownMethod.invoke(context.getRequiredTestClass());
            System.err.println("Invoking teardown method for " + annotatedTestClass.getName());

            atomicInteger = null;
            annotatedTestClass = null;
            setUpMethod = null;
            tearDownMethod = null;
        }
    }

    private static Class findClassWithForAllSubclassesAnnotation(ExtensionContext extensionContext) {
        return findClassWithForAllSubclassesAnnotation(extensionContext.getRequiredTestClass());
    }

    private static Class findClassWithForAllSubclassesAnnotation(Class clazz) {
        if (clazz == null) {
            throw new IllegalStateException("Could not find class with CleanupAfterSubclassesExtension");
        }

        final ExtendWith declaredAnnotation = (ExtendWith) clazz.getDeclaredAnnotation(ExtendWith.class);
        if (declaredAnnotation != null && Arrays.asList(declaredAnnotation.value()).contains(ForAllSubclassesExtension.class)) {
            return clazz;
        }
        return findClassWithForAllSubclassesAnnotation(clazz.getSuperclass());
    }

    private static Method findMethodWithAnnotation(Class clazz, Class<? extends Annotation> annotation) {
        return Arrays.stream(annotatedTestClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotation))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Exactly one method should be annotated with " + annotation.getSimpleName()));
    }
}
