package org.jobrunr.stubs;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobDashboardProgressBar;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.filters.ApplyStateFilter;
import org.jobrunr.jobs.filters.ElectStateFilter;
import org.jobrunr.jobs.filters.JobServerFilter;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.scheduling.BackgroundJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;

public class TestService implements TestServiceInterface {

    private static final Logger LOGGER = new JobRunrDashboardLogger(LoggerFactory.getLogger(TestService.class));

    private static int processedJobs = 0;

    public int getProcessedJobs() {
        return processedJobs;
    }

    public static void doStaticWork() {
        LOGGER.debug("Doing some work from a static method... ");
    }

    public void doWork(Runnable runnable) throws Exception {
        runnable.run();
    }

    public void doWorkWithCommand(Command command) throws Exception {
        LOGGER.debug("Doing some work... ");
        command.doWork();
    }

    public void doWorkWithFile(File file) throws Exception {
        LOGGER.debug("Doing some work... " + file.getAbsolutePath());
    }

    public void doWorkWithPath(Path path) throws Exception {
        LOGGER.debug("Doing some work... " + path.toFile().getAbsolutePath());
    }

    public void doWork(Work work) throws Exception {
        processedJobs += work.workCount;
        LOGGER.debug("Doing some work... " + work.workCount + "; " + work.someString);
    }

    public void doWork(Double count) {
        LOGGER.debug("Doing some work... " + processedJobs + count);
    }

    public void doWork(double[] xValues, double[] yValues) {
        LOGGER.debug("Doing some work with coordinates... ");
    }

    public void doWork(Integer count) {
        processedJobs += count;
        LOGGER.debug("Doing some work... " + processedJobs + "; " + now());
    }

    public void doWork(Long count) {
        processedJobs += count;
        LOGGER.debug("Doing some work... " + processedJobs);
    }

    public void doWork(Integer count, JobContext jobContext) throws InterruptedException {
        processedJobs += count;
        LOGGER.debug("Doing some work... " + processedJobs + "; jobId: " + jobContext.getJobId());
        jobContext.saveMetadata("test", "test");
        Thread.sleep(600L);
    }

    public void doWork(int countA, int countB) {
        processedJobs += (countA + countB);
        LOGGER.debug("Doing some work... " + processedJobs);
    }

    @Job(name = "Doing some hard work for user %1 (customerId: %X{customer.id})")
    public void doWorkWithAnnotation(Integer userId, String userName) {
        LOGGER.debug("Doing some work... " + processedJobs);
    }

    @Job(name = "Doing some hard work for user %1 with id %0")
    public void doWorkWithAnnotationAndJobContext(Integer userId, String userName, JobContext jobContext) {
        LOGGER.debug("Doing some work... " + processedJobs);
    }

    public void doWork(int count, String aString, Instant instant) {
        processedJobs += count;
        LOGGER.debug("Doing some work... " + processedJobs + " " + aString + " " + instant);
    }

    public void doWork(UUID uuid) {
        LOGGER.debug("Doing some work... " + uuid);
    }

    public void doWorkWithUUID(UUID uuid) {
        LOGGER.debug("Doing some work... " + uuid);
    }

    public void doWorkWithLong(Long value) {
        LOGGER.debug("Doing some work... " + value);
    }

    public void doWork(UUID uuid, int count, Instant instant) {
        processedJobs += count;
        LOGGER.debug("Doing some work... " + processedJobs + " " + uuid + " " + instant);
    }

    public void doWork(String aString, int count, Instant instant) {
        processedJobs += count;
        LOGGER.debug("Doing some work... " + processedJobs + " " + aString + " " + instant);
    }

    public void doWork(LocalDateTime localDateTime) {
        LOGGER.debug("Doing some work... " + processedJobs + " " + localDateTime.toString());
    }

    public void doWork(boolean bool, int i, long l, float f, double d) {
        LOGGER.debug("Doing some work... " + bool + "; " + i + "; " + l + "; " + f + "; " + d);
    }

    public void doWork(byte b, short s, char c) {
        LOGGER.debug("Doing some work... " + b + "; " + s + "; " + c);
    }

    public void doWorkWithEnum(Task task) {
        LOGGER.debug("Doing some work: " + task.executeTask());
    }

    @Job(name = "Doing some work")
    public void doWork() {
        processedJobs++;
        LOGGER.debug("Doing some work... " + processedJobs);
    }

    @Job(name = "Doing some work with input")
    public void doWork(String input) {
        LOGGER.debug("Doing some work with input " + input);
    }

    @Job(labels = "label-%0 - %1")
    public void doWorkWithJobAnnotationAndLabels(int i, String s) {
        processedJobs++;
        LOGGER.debug("Doing some work... " + processedJobs);
    }

    @Job(jobFilters = {TheSunIsAlwaysShiningElectStateFilter.class, TestFilter.class})
    public void doWorkWithCustomJobFilters() {
        LOGGER.debug("I will always succeed thanks to my SunIsAlwaysShiningElectStateFilter... ");
    }

    @Job(jobFilters = {JobFilterWithNoDefaultConstructor.class})
    public void doWorkWithCustomJobFilterThatNeedsDependencyInjection() {
        LOGGER.debug("I will never succeed... ");
    }

    public String doWorkAndReturnResult(String someString) {
        return "Hello to you to " + someString;
    }

    @Job(name = "Doing some work", retries = 1, labels = {"Just a label", "Another label"})
    public void doWorkThatFails() {
        processedJobs++;
        LOGGER.debug("Whoopsie, an error will occur " + processedJobs);
        throw new RuntimeException("Whoopsie, an error occurred");
    }

    public void doWorkThatTakesLong(JobContext jobContext) throws InterruptedException {
        final JobDashboardProgressBar progressBar = jobContext.progressBar(9);
        for (int i = 0; i < 10; i++) {
            jobContext.logger().info("This is an info message test " + i);
            Thread.sleep(100);
            jobContext.logger().warn("This is an warning message test " + i);
            Thread.sleep(100);
            jobContext.logger().error("This is an error message test " + i);
            Thread.sleep(100);
            jobContext.logger().info("This is an info message again " + i);
            Thread.sleep(100);
            doWorkThatTakesLong(5 + ThreadLocalRandom.current().nextInt(0, 5));
            progressBar.setValue(i);
        }
    }

    public void doWorkThatTakesLong(int seconds) throws InterruptedException {
        try {
            TimeUnit.SECONDS.sleep(seconds);
            LOGGER.debug("WORK IS DONE!!!!!!!!");
        } catch (InterruptedException e) {
            LOGGER.debug("Thread has been interrupted");
            throw e;
        }
    }

    public void doWorkThatTakesLongInterruptThread(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
            LOGGER.debug("WORK IS DONE!!!!!!!!");
        } catch (InterruptedException e) {
            LOGGER.debug("Thread has been interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public void doWorkThatTakesLongCatchInterruptException(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
            LOGGER.debug("WORK IS DONE!!!!!!!!");
        } catch (InterruptedException e) {
            LOGGER.debug("Thread has been interrupted - not rethrowing nor interrupting again");
        }
    }

    public void doWorkThatCanBeInterrupted(int seconds) throws InterruptedException {
        final Instant start = now();
        long initialNbr = 0;
        while (start.plusSeconds(seconds).isAfter(now())) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            if (Duration.between(start, now()).getSeconds() > initialNbr) {
                LOGGER.debug("WORK IS BEING DONE: " + Duration.between(start, now()).getSeconds());
                initialNbr = Duration.between(start, now()).getSeconds();
            }
        }
    }

    public void doWorkThatTakesLong(long seconds) throws InterruptedException {
        doWorkThatTakesLong((int) seconds);
    }

    public void scheduleNewWork(int amount) {
        scheduleNewWork(amount, -1);
    }

    public void scheduleNewWork(int amount, int exceptionOnNbr) {
        for (int i = 0; i < amount; i++) {
            if (i == exceptionOnNbr) {
                throw new IllegalStateException("An error has occurred processing item " + i);
            } else {
                int finalI = i;
                BackgroundJob.enqueue(() -> doWork(finalI));
            }
        }
    }

    public void scheduleNewWorkSlowly(int amount) throws InterruptedException {
        for (int i = 0; i < amount; i++) {
            int finalI = i;
            BackgroundJob.enqueue(() -> doWork(finalI));
            Thread.sleep(1400);
        }
        System.out.println("scheduleNewWorkSlowly is done");
    }

    @Job(jobFilters = {SkipProcessingElectStateFilter.class})
    public void tryToDoWorkButDontBecauseOfSomeBusinessRuleDefinedInTheOnStateElectionFilter() {
        LOGGER.debug("This should not be executed");
    }

    public void doIllegalWork(IllegalWork illegalWork) {
        LOGGER.debug("Doing some illegal work:" + illegalWork);
    }

    public void doWorkWithoutParameters() {
        doWork(); // why: for kotlin method resolution
    }

    public void reset() {
        processedJobs = 0;
    }

    public UUID getAnUUID() {
        return UUID.randomUUID();
    }

    private void aPrivateMethod(String string, int someNumber) {
        LOGGER.debug("Nothing to do");
    }

    public void jobRunBatchWrappers(Long id, Long env, String param, String currentLogin) {
        LOGGER.debug("Do work:" + id + "; " + env + "; " + param + "; " + currentLogin);
    }

    public void jobRunBatchPrimitives(long id, long env, String param, String currentLogin) {
        LOGGER.debug("Do work:" + id + "; " + env + "; " + param + "; " + currentLogin);
    }

    public static void doWorkInStaticMethod(UUID id) {
        LOGGER.debug("Doing work in static method:" + id);
    }

    public void doWorkWithCollection(Set<Long> singleton) {
        LOGGER.debug("Doing work with collections: " + singleton.size());
    }

    public void doWorkWithMDC(String key) {
        assertThat(MDC.get(key)).isNotNull();
        String result = key + ": " + MDC.get(key) + "; ";
        LOGGER.debug("Found following MDC keys: " + result);
    }

    public void doWorkWithPrimitiveInt(int intValue) {
        LOGGER.debug("Doing some work with a primitive int: " + intValue);
    }

    public void doWorkForIssue645(Long id, GithubIssue645 someObject) {
        LOGGER.debug("Doing work for github issue 645 " + id.toString() + "; " + someObject);
    }

    public static class Work {

        private int workCount;
        private String someString;
        private UUID uuid;

        protected Work() {

        }

        public Work(int workCount, String someString, UUID uuid) {
            this.workCount = workCount;
            this.someString = someString;
            this.uuid = uuid;
        }

        public int getWorkCount() {
            return workCount;
        }

        public String getSomeString() {
            return someString;
        }

        public UUID getUuid() {
            return uuid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Work work = (Work) o;

            if (workCount != work.workCount) return false;
            if (!Objects.equals(someString, work.someString)) return false;
            return Objects.equals(uuid, work.uuid);
        }

        @Override
        public int hashCode() {
            int result = workCount;
            result = 31 * result + (someString != null ? someString.hashCode() : 0);
            result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
            return result;
        }

        public static Work from(int workCount, String someString, UUID uuid) {
            return new Work(workCount, someString, uuid);
        }
    }

    public static class IllegalWork {
        private long number;
        private IllegalWork illegalWork;

        public IllegalWork(long number) {
            this.number = number;
            this.illegalWork = this;
        }

        public IllegalWork getIllegalWork() {
            return illegalWork;
        }

        public long getNumber() {
            return number;
        }
    }

    public static class TheSunIsAlwaysShiningElectStateFilter implements ElectStateFilter {

        @Override
        public void onStateElection(org.jobrunr.jobs.Job job, JobState newState) {
            if (ENQUEUED.equals(newState.getName())) {
                job.succeeded();
            }
        }
    }

    public static class JobFilterWithNoDefaultConstructor implements JobServerFilter {

        public JobFilterWithNoDefaultConstructor(String justAnArgumentSoAnExceptionIsThrown) {
            // filters in the free version need a no-arg constructor
        }
    }

    public static class FailedToDeleteElectStateFilter implements ElectStateFilter {

        @Override
        public void onStateElection(org.jobrunr.jobs.Job job, JobState newState) {
            if (FAILED.equals(newState.getName())) {
                job.delete("Because it failed");
            }
        }
    }

    public static class SkipProcessingElectStateFilter implements ElectStateFilter {

        @Override
        public void onStateElection(org.jobrunr.jobs.Job job, JobState newState) {
            if (PROCESSING.equals(newState.getName())) {
                job.delete("Should not run due to business rule.");
                job.scheduleAt(now().plusSeconds(20), "Rescheduled by business rule.");
            }
        }
    }

    public static class TestFilter implements JobServerFilter, ApplyStateFilter {

        @Override
        public void onStateApplied(org.jobrunr.jobs.Job job, JobState oldState, JobState newState) {
            job.getMetadata().put("onStateApplied", "");
        }

        @Override
        public void onProcessing(org.jobrunr.jobs.Job job) {
            job.getMetadata().put("onProcessing", "");
        }

    }

    public interface Command<T> {
        T doWork();
    }

    public static class SimpleCommand implements Command<Void> {

        private String string;
        private int integer;

        protected SimpleCommand() {

        }

        public SimpleCommand(String string, int integer) {
            this.string = string;
            this.integer = integer;
        }

        @Override
        public Void doWork() {
            LOGGER.debug("Simple Command " + string + " " + integer);
            return null;
        }
    }

    public static class GithubIssue335 {

        public void run(UUID id) {
            LOGGER.debug("Running job for issue 335 " + id);
        }

    }

    public static class GithubIssue645 {
        private Long id;

        public GithubIssue645() {
            this.id = 2L;
        }

        public Long getId() {
            return id;
        }
    }

    public enum Task {

        PROGRAMMING {
            @Override
            public String executeTask() {
                return "In the zone";
            }
        },
        CLEANING {
            @Override
            public String executeTask() {
                return "Cleaning the house - wishing I were in zone";
            }
        };

        public abstract String executeTask();
    }
}
