package org.jobrunr.stubs;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobDashboardProgressBar;
import org.jobrunr.jobs.filters.ApplyStateFilter;
import org.jobrunr.jobs.filters.ElectStateFilter;
import org.jobrunr.jobs.filters.JobServerFilter;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.scheduling.BackgroundJob;

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

import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;

public class TestService implements TestServiceInterface {

    private static int processedJobs = 0;

    public int getProcessedJobs() {
        return processedJobs;
    }

    public static void doStaticWork() {
        System.out.println("Doing some work from a static method... ");
    }

    public void doWork(Runnable runnable) throws Exception {
        runnable.run();
    }

    public void doWorkWithCommand(Command command) throws Exception {
        System.out.println("Doing some work... ");
        command.doWork();
    }

    public void doWorkWithFile(File file) throws Exception {
        System.out.println("Doing some work... " + file.getAbsolutePath());
    }

    public void doWorkWithPath(Path path) throws Exception {
        System.out.println("Doing some work... " + path.toFile().getAbsolutePath());
    }

    public void doWork(Work work) throws Exception {
        processedJobs += work.workCount;
        System.out.println("Doing some work... " + work.workCount + "; " + work.someString);
    }

    public void doWork(Double count) {
        System.out.println("Doing some work... " + processedJobs + count);
    }

    public void doWork(Integer count) {
        processedJobs += count;
        System.out.println("Doing some work... " + processedJobs);
    }

    public void doWork(Long count) {
        processedJobs += count;
        System.out.println("Doing some work... " + processedJobs);
    }

    public void doWork(Integer count, JobContext jobContext) {
        processedJobs += count;
        System.out.println("Doing some work... " + processedJobs + "; jobId: " + jobContext.getJobId());
        jobContext.saveMetadata("test", "test");
    }

    public void doWork(int countA, int countB) {
        processedJobs += (countA + countB);
        System.out.println("Doing some work... " + processedJobs);
    }

    @Job(name = "Doing some hard work for user %1")
    public void doWorkWithAnnotation(Integer userId, String userName) {
        System.out.println("Doing some work... " + processedJobs);
    }

    public void doWork(int count, String aString, Instant instant) {
        processedJobs += count;
        System.out.println("Doing some work... " + processedJobs + " " + aString + " " + instant);
    }

    public void doWork(UUID uuid) {
        System.out.println("Doing some work... " + uuid);
    }

    public void doWorkWithUUID(UUID uuid) {
        System.out.println("Doing some work... " + uuid);
    }

    public void doWorkWithLong(Long value) {
        System.out.println("Doing some work... " + value);
    }

    public void doWork(UUID uuid, int count, Instant instant) {
        processedJobs += count;
        System.out.println("Doing some work... " + processedJobs + " " + uuid + " " + instant);
    }

    public void doWork(String aString, int count, Instant instant) {
        processedJobs += count;
        System.out.println("Doing some work... " + processedJobs + " " + aString + " " + instant);
    }

    public void doWork(LocalDateTime localDateTime) {
        System.out.println("Doing some work... " + processedJobs + " " + localDateTime.toString());
    }

    public void doWork(boolean bool, int i, long l, float f, double d) {
        System.out.println("Doing some work... " + bool + "; " + i + "; " + l + "; " + f + "; " + d);
    }

    public void doWork(byte b, short s, char c) {
        System.out.println("Doing some work... " + b + "; " + s + "; " + c);
    }

    @Job(name = "Doing some work")
    public void doWork() {
        processedJobs++;
        System.out.println("Doing some work... " + processedJobs);
    }

    @Job(jobFilters = {TheSunIsAlwaysShiningElectStateFilter.class, TestFilter.class})
    public void doWorkWithCustomJobFilters() {
        System.out.println("I will always succeed thanks to my SunIsAlwaysShiningElectStateFilter... ");
    }

    public String doWorkAndReturnResult(String someString) {
        return "Hello to you to " + someString;
    }

    @Job(name = "Doing some work", retries = 1)
    public void doWorkThatFails() {
        processedJobs++;
        System.out.println("Whoopsie, an error will occur " + processedJobs);
        throw new RuntimeException("Whoopsie, an error occcured");
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
            System.out.println("WORK IS DONE!!!!!!!!");
        } catch (InterruptedException e) {
            System.out.println("Thread has been interrupted");
            throw e;
        }
    }

    public void doWorkThatTakesLongInterruptThread(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
            System.out.println("WORK IS DONE!!!!!!!!");
        } catch (InterruptedException e) {
            System.out.println("Thread has been interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public void doWorkThatTakesLongCatchInterruptException(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
            System.out.println("WORK IS DONE!!!!!!!!");
        } catch (InterruptedException e) {
            System.out.println("Thread has been interrupted - not rethrowing nor interrupting again");
        }
    }

    public void doWorkThatCanBeInterrupted(int seconds) throws InterruptedException {
        final Instant start = Instant.now();
        long initialNbr = 0;
        while (start.plusSeconds(seconds).isAfter(Instant.now())) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            if (Duration.between(start, Instant.now()).getSeconds() > initialNbr) {
                System.out.println("WORK IS BEING DONE: " + Duration.between(start, Instant.now()).getSeconds());
                initialNbr = Duration.between(start, Instant.now()).getSeconds();
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

    public void scheduleNewWorkSlowly(int amount) {
        try {
            for (int i = 0; i < amount; i++) {
                int finalI = i;
                BackgroundJob.enqueue(() -> doWork(finalI));
                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Job(jobFilters = {SkipProcessingElectStateFilter.class})
    public void tryToDoWorkButDontBecauseOfSomeBusinessRuleDefinedInTheOnStateElectionFilter() {
        System.out.println("This should not be executed");
    }

    public void doIllegalWork(IllegalWork illegalWork) {
        System.out.println("Doing some illegal work:" + illegalWork);
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
        System.out.println("Nothing to do");
    }

    public void jobRunBatchWrappers(Long id, Long env, String param, String currentLogin) {
        System.out.println("Do work:" + id + "; " + env + "; " + param + "; " + currentLogin);
    }

    public void jobRunBatchPrimitives(long id, long env, String param, String currentLogin) {
        System.out.println("Do work:" + id + "; " + env + "; " + param + "; " + currentLogin);
    }

    public static void doWorkInStaticMethod(UUID id) {
        System.out.println("Doing work in static method:" + id);
    }

    public void doWorkWithCollection(Set<Long> singleton) {
        System.out.println("Doing work with collections: " + singleton.size());
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
                job.scheduleAt(Instant.now(), "Rescheduled by business rule.");
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

        @Override
        public void onProcessed(org.jobrunr.jobs.Job job) {
            job.getMetadata().put("onProcessed", "");
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
            System.out.println("Simple Command " + string + " " + integer);
            return null;
        }
    }

    public static class GithubIssue335 {

        public void run(UUID id) {
            System.out.println("Running job for issue 335 " + id);
        }

    }
}
