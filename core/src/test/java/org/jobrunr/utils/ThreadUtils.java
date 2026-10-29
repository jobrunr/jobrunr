package org.jobrunr.utils;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ThreadUtils {

    public static void assertNoBackgroundJobServerThreadsExist() {
        assertNoBackgroundJobServerThreadsExist("");
    }

    public static void assertNoBackgroundJobServerThreadsExist(String description) {
        String actualDescription = StringUtils.isNotNullOrEmpty(description) ? " (" + description + ")" : "";
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(Thread.getAllStackTraces())
                        .matches(ThreadUtils::containsNoBackgroundJobThreads, "Found BackgroundJob Threads" + actualDescription + ": \n\t" + getThreadNames(Thread.getAllStackTraces()).collect(Collectors.joining("\n\t"))));
    }

    public static boolean containsBackgroundJobThreads(Map<Thread, StackTraceElement[]> threadMap) {
        return getThreadNames(threadMap).anyMatch(threadName -> threadName.startsWith("backgroundjob"));
    }

    public static boolean containsNoBackgroundJobThreads(Map<Thread, StackTraceElement[]> threadMap) {
        return getThreadNames(threadMap).noneMatch(threadName -> threadName.startsWith("backgroundjob"));
    }

    private static Stream<String> getThreadNames(Map<Thread, StackTraceElement[]> threadMap) {
        return threadMap.keySet().stream().map(Thread::getName);
    }
}
