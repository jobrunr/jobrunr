package org.jobrunr.server;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.filters.RetryFilter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

public class BackgroundJobServerAssert extends AbstractAssert<BackgroundJobServerAssert, BackgroundJobServer> {
    protected BackgroundJobServerAssert(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer, BackgroundJobServerAssert.class);
    }

    public static BackgroundJobServerAssert assertThat(BackgroundJobServer backgroundJobServer) {
        return new BackgroundJobServerAssert(backgroundJobServer);
    }

    public BackgroundJobServerAssert hasRetryFilter(int defaultNbrOfRetries) {
        List<JobFilter> filters = getInternalState(actual.getJobFilters(), "filters");
        JobFilter retryFilter = filters.stream()
                .filter(filter -> filter instanceof RetryFilter)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("No retry filter found"));
        Assertions.assertThat((Integer) getInternalState(retryFilter, "numberOfRetries")).isEqualTo(defaultNbrOfRetries);
        return this;
    }

    public BackgroundJobServerAssert hasRunningBackgroundJobThreads() {
        await().atMost(TEN_SECONDS).untilAsserted(() -> Assertions.assertThat(Thread.getAllStackTraces()).matches(this::containsBackgroundJobThreads));
        return this;
    }

    public BackgroundJobServerAssert hasNoRunningBackgroundJobThreads() {
        await().atMost(ONE_MINUTE).untilAsserted(() -> Assertions.assertThat(Thread.getAllStackTraces())
                        .matches(this::containsNoBackgroundJobThreads, "Found BackgroundJob Threads: \n\t" + getThreadNames(Thread.getAllStackTraces()).collect(Collectors.joining("\n\t"))));
        return this;
    }

    public BackgroundJobServerAssert hasJobFilterOfType(Class<? extends JobFilter> jobFilterClass) {
        List<JobFilter> filters = getInternalState(actual.getJobFilters(), "filters");
        Assertions.assertThat(filters).anyMatch(jobFilter -> jobFilterClass.equals(jobFilter.getClass()));
        return this;
    }

    private boolean containsBackgroundJobThreads(Map<Thread, StackTraceElement[]> threadMap) {
        return getThreadNames(threadMap).anyMatch(threadName -> threadName.startsWith("backgroundjob"));
    }

    private boolean containsNoBackgroundJobThreads(Map<Thread, StackTraceElement[]> threadMap) {
        return getThreadNames(threadMap).noneMatch(threadName -> threadName.startsWith("backgroundjob"));
    }

    private Stream<String> getThreadNames(Map<Thread, StackTraceElement[]> threadMap) {
        return threadMap.keySet().stream().map(Thread::getName);
    }
}
