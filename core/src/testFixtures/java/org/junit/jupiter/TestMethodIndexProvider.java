package org.junit.jupiter;

public class TestMethodIndexProvider {

    private static Class<?> currentTestClass;

    private static int testMethodIndex;

    public static int getTestMethodIndex(Class<?> currentTestClass) {
        if (TestMethodIndexProvider.currentTestClass != currentTestClass) {
            testMethodIndex = 0;
        }
        TestMethodIndexProvider.currentTestClass = currentTestClass;
        return testMethodIndex++;
    }

}
