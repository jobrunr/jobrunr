package org.jobrunr.jobs.details;

import org.assertj.core.api.Assertions;
import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.TestServiceInterface;
import org.jobrunr.utils.annotations.Because;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.util.Textifier;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQResource;

public abstract class AbstractJobDetailsGeneratorTest {

    private TestService testService;
    private TestServiceInterface testServiceInterface;
    private JobDetailsGenerator jobDetailsGenerator;

    private enum SomeEnum {
        Value1,
        Value2
    }

    @BeforeEach
    void setUp() {
        jobDetailsGenerator = getJobDetailsGenerator();
        testService = new TestService();
        testServiceInterface = testService;
    }

    protected abstract JobDetailsGenerator getJobDetailsGenerator();

    protected JobDetails toJobDetails(JobLambda job) {
        return jobDetailsGenerator.toJobDetails(job);
    }

    protected JobDetails toJobDetails(IocJobLambda<TestService> iocJobLambda) {
        return jobDetailsGenerator.toJobDetails(iocJobLambda);
    }

    @Test
    @Disabled("to much logging during build")
    void logByteCode() {
        String name = AbstractJobDetailsGeneratorTest.class.getName();
        String location = new File(".").getAbsolutePath() + "/build/classes/java/test/" + toFQResource(name) + ".class";

        //String location = "/home/ronald/Projects/Personal/JobRunr/jobrunr/jobrunr-kotlin-15-support/build/classes/kotlin/test/org/jobrunr/jobs/details/JobDetailsAsmGeneratorForKotlinTest$testMethodReferenceJobLambdaInSameClass$jobDetails$1.class";
        assertThatCode(() -> Textifier.main(new String[]{location})).doesNotThrowAnyException();
    }

    @Test
    void testJobLambdaCallingSystemOutPrintln() {
        JobLambda job = () -> System.out.println("This is a test!");
        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(System.class)
                .hasStaticFieldName("out")
                .hasMethodName("println")
                .hasArgs("This is a test!");
    }

    @Test
    void testJobLambdaCallingInlineSystemOutPrintln() {
        JobDetails jobDetails = toJobDetails((JobLambda) () -> System.out.println("This is a test!"));
        assertThat(jobDetails)
                .hasClass(System.class)
                .hasStaticFieldName("out")
                .hasMethodName("println")
                .hasArgs("This is a test!");
    }

    @Test
    void testJobLambdaContainingTwoStaticJobsShouldFailWithNiceException() {
        final JobLambda jobLambda = () -> {
            System.out.println("This is a test!");
            System.out.println("This is a test!");
        };
        assertThatThrownBy(() -> toJobDetails(jobLambda))
                .isInstanceOf(JobRunrException.class)
                .hasMessage("JobRunr only supports enqueueing/scheduling of one method");
    }

    @Test
    void testJobLambdaContainingTwoJobsShouldFailWithNiceException() {
        final JobLambda jobLambda = () -> {
            testService.doWork();
            testService.doWork();
        };
        assertThatThrownBy(() -> toJobDetails(jobLambda))
                .isInstanceOf(JobRunrException.class)
                .hasMessage("JobRunr only supports enqueueing/scheduling of one method");
    }

    @Test
    void testJobLambdaCallingStaticMethod() {
        UUID id = UUID.randomUUID();
        JobLambda job = () -> TestService.doWorkInStaticMethod(id);
        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWorkInStaticMethod")
                .hasArgs(id);
    }

    @Test
    void testJobLambdaCallingInlineStaticMethod() {
        UUID id = UUID.randomUUID();
        JobDetails jobDetails = toJobDetails(() -> TestService.doWorkInStaticMethod(id));
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWorkInStaticMethod")
                .hasArgs(id);
    }

    @Test
    void testJobLambdaCallMethodReference() {
        JobLambda job = testService::doWork;
        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasNoArgs();
    }

    @Test
    void testJobLambdaCallInstanceMethod_Null() {
        TestService.Work work = null;
        JobLambda job = () -> testService.doWork(work);
        assertThatThrownBy(() -> toJobDetails(job))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("You are passing null as a parameter to your background job for type org.jobrunr.stubs.TestService$Work - JobRunr prevents this to fail fast.");
    }

    @Test
    void testJobLambdaCallInstanceMethod_OtherLambda() {
        Supplier<Boolean> supplier = () -> {
            System.out.println("Dit is een test");
            return true;
        };
        JobLambda job = () -> {
            if (supplier.get()) {
                System.out.println("In nested lambda");
            }
        };
        assertThatThrownBy(() -> toJobDetails(job))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You are passing another (nested) Java 8 lambda to JobRunr - this is not supported. Try to convert your lambda to a class or a method.");
    }

    @Test
    void testJobLambdaCallInstanceMethod_BIPUSH() {
        JobLambda job = () -> testService.doWork(5);
        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(5);
    }

    @Test
    void testJobLambdaCallInstanceMethod_SIPUSH() {
        JobLambda job = () -> testService.doWorkThatTakesLong(500);
        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWorkThatTakesLong")
                .hasArgs(500);
    }

    @Test
    void testJobLambdaCallInstanceMethod_LCONST() {
        JobLambda job = () -> testService.doWorkThatTakesLong(1L);
        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWorkThatTakesLong")
                .hasArgs(1L);
    }

    @Test
    void testInlineJobLambdaCallInstanceMethod() {
        JobDetails jobDetails = toJobDetails((JobLambda) () -> testService.doWork());
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasNoArgs();
    }

    @Test
    void testJobLambdaWithIntegerAndJobContext() {
        JobLambda job = () -> testService.doWork(3, JobContext.Null);
        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3, JobContext.Null);
    }

    @Test
    void testJobLambdaWithDouble() {
        JobLambda job = () -> testService.doWork(3.3);
        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3.3);
    }

    @Test
    void testJobLambdaWithSum() {
        int a = 6;
        int b = 3;
        JobLambda job = () -> testService.doWork(a + b);

        assertThatCode(() -> toJobDetails(job))
                .isInstanceOf(IllegalArgumentException.class)
                .hasRootCauseInstanceOf(UnsupportedOperationException.class)
                .hasRootCauseMessage("You are summing two numbers while enqueueing/scheduling jobs - for performance reasons it is better to do the calculation outside of the job lambda");
    }

    @Test
    void testJobLambdaWithSubtraction() {
        int a = 6;
        int b = 3;
        JobLambda job = () -> testService.doWork(a - b);

        assertThatCode(() -> toJobDetails(job))
                .isInstanceOf(IllegalArgumentException.class)
                .hasRootCauseInstanceOf(UnsupportedOperationException.class)
                .hasRootCauseMessage("You are subtracting two numbers while enqueueing/scheduling jobs - for performance reasons it is better to do the calculation outside of the job lambda");
    }

    @Test
    void testJobLambdaWithMultiplication() {
        int a = 6;
        int b = 3;
        JobLambda job = () -> testService.doWork(a * b);

        assertThatCode(() -> toJobDetails(job))
                .isInstanceOf(IllegalArgumentException.class)
                .hasRootCauseInstanceOf(UnsupportedOperationException.class)
                .hasRootCauseMessage("You are multiplying two numbers while enqueueing/scheduling jobs - for performance reasons it is better to do the calculation outside of the job lambda");
    }

    @Test
    void testJobLambdaWithDivision() {
        int a = 6;
        int b = 3;
        JobLambda job = () -> testService.doWork(a / b);

        assertThatCode(() -> toJobDetails(job))
                .isInstanceOf(IllegalArgumentException.class)
                .hasRootCauseInstanceOf(UnsupportedOperationException.class)
                .hasRootCauseMessage("You are dividing two numbers while enqueueing/scheduling jobs - for performance reasons it is better to do the calculation outside of the job lambda");
    }

    @Test
    void testJobLambdaWithMultipleInts() {
        JobLambda job = () -> testService.doWork(3, 97693);
        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3, 97693);
    }

    @Test
    void testJobLambdaWithMultipleParameters() {
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            Instant now = Instant.now();
            JobLambda job = () -> testService.doWork("some string", finalI, now);

            JobDetails jobDetails = toJobDetails(job);
            assertThat(jobDetails)
                    .hasClass(TestService.class)
                    .hasMethodName("doWork")
                    .hasArgs("some string", finalI, now);
        }
    }

    @Test
    void testJobLambdaWithObject() {
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            JobLambda job = () -> testService.doWork(new TestService.Work(finalI, "a String", UUID.randomUUID()));

            JobDetails jobDetails = toJobDetails(job);
            assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWork");
            JobParameter jobParameter = jobDetails.getJobParameters().get(0);
            Assertions.assertThat(jobParameter.getClassName()).isEqualTo(TestService.Work.class.getName());
            Assertions.assertThat(jobParameter.getObject())
                    .isInstanceOf(TestService.Work.class)
                    .hasFieldOrPropertyWithValue("workCount", finalI)
                    .hasFieldOrPropertyWithValue("someString", "a String")
                    .hasFieldOrProperty("uuid");
        }
    }

    @Test
    void testJobLambdaWithFile() {
        JobLambda job = () -> testService.doWorkWithFile(new File("/tmp/file.txt"));

        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithFile");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        Assertions.assertThat(jobParameter.getClassName()).isEqualTo(File.class.getName());
        Assertions.assertThat(jobParameter.getObject())
                .isInstanceOf(File.class)
                .isEqualTo(new File("/tmp/file.txt"));
    }

    @Test
    void testJobLambdaWithPath() {
        Path path = Paths.get("/tmp/file.txt");
        JobLambda job = () -> testService.doWorkWithPath(path);

        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithPath");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        Assertions.assertThat(jobParameter.getClassName()).isEqualTo(Path.class.getName());
        Assertions.assertThat(jobParameter.getObject())
                .isInstanceOf(Path.class)
                .isEqualTo(path);
    }

    @Test
    void testJobLambdaWithPathsGetInLambda() {
        JobLambda job = () -> testService.doWorkWithPath(Paths.get("/tmp/file.txt"));

        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithPath");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        Assertions.assertThat(jobParameter.getClassName()).isEqualTo(Path.class.getName());
        Assertions.assertThat(jobParameter.getObject())
                .isInstanceOf(Path.class)
                .isEqualTo(Paths.get("/tmp/file.txt"));
    }

    @Test
    void testJobLambdaWithPaths() {
        for (int i = 0; i < 3; i++) {
            final Path path = Paths.get("/tmp/file" + i + ".txt");
            JobLambda job = () -> testService.doWorkWithPath(path);

            JobDetails jobDetails = toJobDetails(job);
            assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithPath");
            JobParameter jobParameter = jobDetails.getJobParameters().get(0);
            Assertions.assertThat(jobParameter.getClassName()).isEqualTo(Path.class.getName());
            Assertions.assertThat(jobParameter.getObject())
                    .isInstanceOf(Path.class)
                    .isEqualTo(path);
        }
    }

    @Test
    void testJobLambdaWithPathsGetMultiplePartsInLambda() {
        JobLambda job = () -> testService.doWorkWithPath(Paths.get("/tmp", "folder", "subfolder", "file.txt"));

        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithPath");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        Assertions.assertThat(jobParameter.getClassName()).isEqualTo(Path.class.getName());
        Assertions.assertThat(jobParameter.getObject())
                .isInstanceOf(Path.class)
                .isEqualTo(Paths.get("/tmp/folder/subfolder/file.txt"));
    }

    @Test
    void testJobLambdaWithPathOfInLambda() {
        JobLambda job = () -> testService.doWorkWithPath(Paths.get("/tmp/file.txt"));

        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithPath");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        Assertions.assertThat(jobParameter.getClassName()).isEqualTo(Path.class.getName());
        Assertions.assertThat(jobParameter.getObject())
                .isInstanceOf(Path.class)
                .isEqualTo(Paths.get("/tmp/file.txt"));
    }

    @Test
    void testJobLambdaWithObjectCreatedOutsideOfLambda() {
        for (int i = 0; i < 3; i++) {
            TestService.Work work = new TestService.Work(i, "a String", UUID.randomUUID());
            JobLambda job = () -> testService.doWork(work);

            JobDetails jobDetails = toJobDetails(job);
            assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWork");
            JobParameter jobParameter = jobDetails.getJobParameters().get(0);
            Assertions.assertThat(jobParameter.getClassName()).isEqualTo(TestService.Work.class.getName());
            Assertions.assertThat(jobParameter.getObject())
                    .isInstanceOf(TestService.Work.class)
                    .hasFieldOrPropertyWithValue("workCount", i)
                    .hasFieldOrPropertyWithValue("someString", "a String")
                    .hasFieldOrPropertyWithValue("uuid", work.getUuid());
        }
    }

    @Test
    void testJobLambdaWithSupportedPrimitiveTypes() {
        JobLambda job = () -> testService.doWork(true, 3, 5L, 3.3F, 2.3D);
        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(true, 3, 5L, 3.3F, 2.3D);
    }

    @Test
    void testJobLambdaWithUnsupportedPrimitiveTypes() {
        JobLambda job = () -> testService.doWork((byte) 0x3, (short) 2, 'c');
        assertThatThrownBy(() -> toJobDetails(job))
                .isInstanceOf(JobRunrException.class)
                .hasMessage("Error parsing lambda")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testJobLambdaCallingMultiLine() {
        final List<UUID> workStream = getWorkStream().collect(toList());
        Instant now = Instant.now();
        for (UUID id : workStream) {
            JobLambda job = () -> {
                int someInt = 6;
                testService.doWork(id, someInt, now);
            };
            JobDetails jobDetails = toJobDetails(job);
            assertThat(jobDetails)
                    .hasClass(TestService.class)
                    .hasMethodName("doWork")
                    .hasArgs(id, 6, now);
        }
    }

    @Test
    void testJobLambdaCallingMultiLineStatementSystemOutPrintln() {
        final List<UUID> workStream = getWorkStream().collect(toList());
        LocalDateTime now = LocalDateTime.now();
        for (UUID id : workStream) {
            JobLambda job = () -> {
                UUID testId = id;
                int someInt = 6;
                double someDouble = 5.3;
                float someFloat = 5.3F;
                long someLong = 3L;
                boolean someBoolean = true;
                SomeEnum someEnum = SomeEnum.Value1;
                System.out.println("This is a test: " + testId + "; " + someInt + "; " + someDouble + "; " + someFloat + "; " + someLong + "; " + someBoolean + "; " + someEnum + "; " + now);
            };
            JobDetails jobDetails = toJobDetails(job);
            assertThat(jobDetails)
                    .hasClass(System.class)
                    .hasStaticFieldName("out")
                    .hasMethodName("println")
                    .hasArg(obj -> obj.toString().startsWith("This is a test: " + id)
                            && obj.toString().contains(" 6; 5.3; 5.3; 3; true; Value1;")
                            && obj.toString().contains(now.toString()));
        }
    }

    @Test
    void testJobLambdaCallingMultiLineStatementThatLoadsFromAService() {
        JobLambda job = () -> {
            UUID testId = testService.getAnUUID();
            testService.doWork(testId);
        };

        JobDetails jobDetails = toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArg(obj -> obj instanceof UUID);
    }

    @Test
    void testJobLambdaWithArgumentThatIsNotUsed() {
        final Stream<Integer> range = IntStream.range(0, 1).boxed();
        assertThatCode(() -> jobDetailsGenerator.toJobDetails(range, (i) -> testService.doWork())).doesNotThrowAnyException();
    }

    @Test
    void testJobLambdaWithStaticMethodInLambda() {
        JobLambda jobLambda = () -> testService.doWork(TestService.Work.from(2, "a String", UUID.randomUUID()));
        final JobDetails jobDetails = toJobDetails(jobLambda);

        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWork");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        Assertions.assertThat(jobParameter.getClassName()).isEqualTo(TestService.Work.class.getName());
        Assertions.assertThat(jobParameter.getObject())
                .isInstanceOf(TestService.Work.class)
                .hasFieldOrPropertyWithValue("workCount", 2)
                .hasFieldOrPropertyWithValue("someString", "a String")
                .hasFieldOrProperty("uuid");
    }

    @Test
    void testJobLambdaWhichReturnsSomething() {
        JobLambda jobLambda = () -> testService.doWorkAndReturnResult("someString");
        JobDetails jobDetails = toJobDetails(jobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWorkAndReturnResult")
                .hasArgs("someString");
    }

    @Test
    void testSimpleJobLambdaWithStream() {
        List<UUID> workStream = getWorkStream().collect(toList());
        final JobLambdaFromStream<UUID> lambda = (uuid) -> testService.doWork(uuid, 3, now());
        final List<JobDetails> allJobDetails = workStream.stream()
                .map(x -> jobDetailsGenerator.toJobDetails(x, lambda))
                .collect(toList());

        Assertions.assertThat(allJobDetails).hasSize(5);
        assertThat(allJobDetails.get(0)).hasClass(TestService.class).hasMethodName("doWork");
        Assertions.assertThat(allJobDetails.get(0).getJobParameters().get(0).getObject()).isEqualTo(workStream.get(0));
        Assertions.assertThat(allJobDetails.get(4).getJobParameters().get(0).getObject()).isEqualTo(workStream.get(4));
    }

    @Test
    void testJobLambdaWithStream() {
        Stream<UUID> workStream = getWorkStream();
        AtomicInteger atomicInteger = new AtomicInteger();
        final JobLambdaFromStream<UUID> lambda = (uuid) -> testService.doWork(uuid.toString(), atomicInteger.incrementAndGet(), now());
        final List<JobDetails> allJobDetails = workStream
                .map(x -> jobDetailsGenerator.toJobDetails(x, lambda))
                .collect(toList());

        Assertions.assertThat(allJobDetails).hasSize(5);
        assertThat(allJobDetails.get(0)).hasClass(TestService.class).hasMethodName("doWork");
        Assertions.assertThat(allJobDetails.get(0).getJobParameters().get(1).getObject()).isEqualTo(1);
        Assertions.assertThat(allJobDetails.get(4).getJobParameters().get(1).getObject()).isEqualTo(5);
    }

    @Test
    void testJobLambdaWithStreamAndObject() {
        List<UUID> workStream = getWorkStream().collect(toUnmodifiableList());
        AtomicInteger atomicInteger = new AtomicInteger();
        final JobLambdaFromStream<TestService.Work> lambda = (work) -> testService.doWork(work);
        final List<JobDetails> allJobDetails = workStream.stream()
                .map(uuid -> new TestService.Work(atomicInteger.getAndIncrement(), Integer.toString(atomicInteger.get()), uuid))
                .map(x -> jobDetailsGenerator.toJobDetails(x, lambda))
                .collect(toList());

        Assertions.assertThat(allJobDetails).hasSize(5);
        assertThat(allJobDetails.get(0)).hasClass(TestService.class).hasMethodName("doWork");
        Assertions.assertThat(allJobDetails.get(0).getJobParameters().get(0).getObject())
                .isInstanceOf(TestService.Work.class)
                .hasFieldOrPropertyWithValue("someString", "1")
                .hasFieldOrPropertyWithValue("uuid", workStream.get(0));
        Assertions.assertThat(allJobDetails.get(4).getJobParameters().get(0).getObject())
                .isInstanceOf(TestService.Work.class)
                .hasFieldOrPropertyWithValue("someString", "5")
                .hasFieldOrPropertyWithValue("uuid", workStream.get(4));
    }

    @Test
    void testJobLambdaWithStreamAndMethodReference() {
        final UUID uuid = UUID.randomUUID();
        final JobDetails jobDetails = jobDetailsGenerator.toJobDetails(uuid, TestService::doWorkWithUUID);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWorkWithUUID")
                .hasArgs(uuid);
    }

    @Test
    void testJobLambdaWithStreamAndMethodReferenceInSameFile() {
        final UUID uuid = UUID.randomUUID();
        final JobDetails jobDetails = jobDetailsGenerator.toJobDetails(uuid, AbstractJobDetailsGeneratorTest::doWorkWithUUID);
        assertThat(jobDetails)
                .hasClass(AbstractJobDetailsGeneratorTest.class)
                .hasMethodName("doWorkWithUUID")
                .hasArgs(uuid);
    }

    @Test
    void testIocJobLambda() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork();
        JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasNoArgs();
    }

    @Test
    void testInlineIocJobLambda() {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails((IocJobLambda<TestService>) (x) -> x.doWork(5));
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(5);
    }

    @Test
    void testIocJobLambdaWithIntegerAndJobContext() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(3, JobContext.Null);
        JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3, JobContext.Null);
    }

    @Test
    void testIocJobLambdaWithDouble() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(3.3);
        JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3.3);
    }

    @Test
    void testIocJobLambdaWithMultipleInts() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(3, 97693);
        JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3, 97693);
    }

    @Test
    void testIocJobLambdaWithMultipleParameters() {
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            Instant now = Instant.now();
            IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork("some string", finalI, now);

            JobDetails jobDetails = toJobDetails(iocJobLambda);
            assertThat(jobDetails)
                    .hasClass(TestService.class)
                    .hasMethodName("doWork")
                    .hasArgs("some string", finalI, now);
        }
    }

    @Test
    void testIocJobLambdaWithObject() {
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(new TestService.Work(finalI, "a String", UUID.randomUUID()));

            JobDetails jobDetails = toJobDetails(iocJobLambda);
            assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWork");
            JobParameter jobParameter = jobDetails.getJobParameters().get(0);
            Assertions.assertThat(jobParameter.getClassName()).isEqualTo(TestService.Work.class.getName());
            Assertions.assertThat(jobParameter.getObject())
                    .isInstanceOf(TestService.Work.class)
                    .hasFieldOrPropertyWithValue("workCount", finalI)
                    .hasFieldOrPropertyWithValue("someString", "a String")
                    .hasFieldOrProperty("uuid");
        }
    }

    @Test
    void testIoCJobLambdaWithFile() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWorkWithFile(new File("/tmp/file.txt"));

        JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithFile");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        Assertions.assertThat(jobParameter.getClassName()).isEqualTo(File.class.getName());
        Assertions.assertThat(jobParameter.getObject())
                .isInstanceOf(File.class)
                .isEqualTo(new File("/tmp/file.txt"));
    }

    @Test
    void testIoCJobLambdaWithPath() {
        Path path = Paths.get("/tmp/file.txt");
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWorkWithPath(path);

        JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithPath");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        Assertions.assertThat(jobParameter.getClassName()).isEqualTo(Path.class.getName());
        Assertions.assertThat(jobParameter.getObject())
                .isInstanceOf(Path.class)
                .isEqualTo(path);
    }

    @Test
    void testIocJobLambdaWithObjectCreatedOutsideOfLambda() {
        for (int i = 0; i < 3; i++) {
            TestService.Work work = new TestService.Work(i, "a String", UUID.randomUUID());
            IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(work);

            JobDetails jobDetails = toJobDetails(iocJobLambda);
            assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWork");
            JobParameter jobParameter = jobDetails.getJobParameters().get(0);
            Assertions.assertThat(jobParameter.getClassName()).isEqualTo(TestService.Work.class.getName());
            Assertions.assertThat(jobParameter.getObject())
                    .isInstanceOf(TestService.Work.class)
                    .hasFieldOrPropertyWithValue("workCount", i)
                    .hasFieldOrPropertyWithValue("someString", "a String")
                    .hasFieldOrProperty("uuid");
        }
    }

    @Test
    void testIocJobLambdaWithSupportedPrimitiveTypes() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(true, 3, 5L, 3.3F, 2.3D);
        JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(true, 3, 5L, 3.3F, 2.3D);
    }

    @Test
    void testIocJobLambdaWithSupportedPrimitiveTypes_LOAD() {
        for (int i = 0; i < 3; i++) {
            final boolean finalB = i % 2 == 0;
            final int finalI = i;
            final long finalL = 5L + i;
            final float finalF = 3.3F + i;
            final double finalD = 2.3D + i;
            IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(finalB, finalI, finalL, finalF, finalD);
            JobDetails jobDetails = toJobDetails(iocJobLambda);
            assertThat(jobDetails)
                    .hasClass(TestService.class)
                    .hasMethodName("doWork")
                    .hasArgs(i % 2 == 0, finalI, 5L + i, 3.3F + i, 2.3D + i);
        }
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/158")
    void testIocJobLambdaWithPrimitiveWrappers_LOAD() {
        for (int i = 0; i < 3; i++) {
            final Boolean finalB = i % 2 == 0;
            final Integer finalI = i;
            final Long finalL = 5L + i;
            final Float finalF = 3.3F + i;
            final Double finalD = 2.3D + i;
            IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(finalB, finalI, finalL, finalF, finalD);
            JobDetails jobDetails = toJobDetails(iocJobLambda);
            assertThat(jobDetails)
                    .hasClass(TestService.class)
                    .hasMethodName("doWork")
                    .hasArgs(i % 2 == 0, finalI, 5L + i, 3.3F + i, 2.3D + i);
        }
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/165")
    void testJobLambdaWithPrimitiveParametersAndWrappersInMethod_LOAD() {
        long id = 1L;
        long env = 2L;
        String param = "test";

        final JobDetails jobDetails = toJobDetails(() -> testService.jobRunBatchWrappers(id, env, param, TestUtils.getCurrentLogin()));
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("jobRunBatchWrappers")
                .hasArgs(1L, 2L, "test", "Some string");
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/165")
    void testIoCJobLambdaWithPrimitiveParametersAndWrappersInMethod_LOAD() {
        long id = 1L;
        long env = 2L;
        String param = "test";

        IocJobLambda<TestService> iocJobLambda = (x) -> x.jobRunBatchWrappers(id, env, param, TestUtils.getCurrentLogin());
        final JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("jobRunBatchWrappers")
                .hasArgs(1L, 2L, "test", "Some string");
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/165")
    void testJobLambdaWithPrimitiveParametersAndPrimitivesInMethod_LOAD() {
        long id = 1L;
        long env = 2L;
        String param = "test";

        final JobDetails jobDetails = toJobDetails(() -> testService.jobRunBatchPrimitives(id, env, param, TestUtils.getCurrentLogin()));
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("jobRunBatchPrimitives")
                .hasArgs(1L, 2L, "test", "Some string");
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/165")
    void testIoCJobLambdaWithPrimitiveParametersAndPrimitivesInMethod_LOAD() {
        long id = 1L;
        long env = 2L;
        String param = "test";

        IocJobLambda<TestService> iocJobLambda = (x) -> x.jobRunBatchPrimitives(id, env, param, TestUtils.getCurrentLogin());
        final JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("jobRunBatchPrimitives")
                .hasArgs(1L, 2L, "test", "Some string");
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/165")
    void testJobLambdaWithCombinationParametersAndPrimitivesInMethod_LOAD() {
        long id = 1L;
        Long env = 2L;
        String param = "test";

        final JobDetails jobDetails = toJobDetails(() -> testService.jobRunBatchPrimitives(id, env, param, TestUtils.getCurrentLogin()));
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("jobRunBatchPrimitives")
                .hasArgs(1L, 2L, "test", "Some string");
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/165")
    void testIoCJobLambdaWithCombinationParametersAndPrimitivesInMethod_LOAD() {
        long id = 1L;
        Long env = 2L;
        String param = "test";

        IocJobLambda<TestService> iocJobLambda = (x) -> x.jobRunBatchPrimitives(id, env, param, TestUtils.getCurrentLogin());
        final JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("jobRunBatchPrimitives")
                .hasArgs(1L, 2L, "test", "Some string");
    }


    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/165")
    void testJobLambdaWithCombinationParametersAndWrappersInMethod_LOAD() {
        long id = 1L;
        Long env = 2L;
        String param = "test";

        final JobDetails jobDetails = toJobDetails(() -> testService.jobRunBatchWrappers(id, env, param, TestUtils.getCurrentLogin()));
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("jobRunBatchWrappers")
                .hasArgs(1L, 2L, "test", "Some string");
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/165")
    void testIoCJobLambdaWithCombinationParametersAndWrappersInMethod_LOAD() {
        long id = 1L;
        Long env = 2L;
        String param = "test";

        IocJobLambda<TestService> iocJobLambda = (x) -> x.jobRunBatchPrimitives(id, env, param, TestUtils.getCurrentLogin());
        final JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("jobRunBatchPrimitives")
                .hasArgs(1L, 2L, "test", "Some string");
    }

    @Test
    void testIocJobLambdaWithUnsupportedPrimitiveTypes() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork((byte) 0x3, (short) 2, 'c');
        assertThatThrownBy(() -> toJobDetails(iocJobLambda))
                .isInstanceOf(JobRunrException.class)
                .hasMessage("Error parsing lambda")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testIocJobLambdaWithArgumentThatIsNotUsed() {
        IocJobLambdaFromStream<TestService, Integer> iocJobLambdaFromStream = (x, i) -> x.doWork();
        assertThatCode(() -> jobDetailsGenerator.toJobDetails(5, iocJobLambdaFromStream)).doesNotThrowAnyException();
    }

    @Test
    void testIocJobLambdaWhichReturnsSomething() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWorkAndReturnResult("someString");
        JobDetails jobDetails = toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWorkAndReturnResult")
                .hasArgs("someString");
    }

    @Test
    void testSimpleIoCJobLambdaWithStream() {
        List<UUID> workStream = getWorkStream().collect(toList());
        final IocJobLambdaFromStream<TestService, UUID> lambda = (service, uuid) -> service.doWork(uuid, 5, now());
        final List<JobDetails> allJobDetails = workStream.stream()
                .map(x -> jobDetailsGenerator.toJobDetails(x, lambda))
                .collect(toList());

        Assertions.assertThat(allJobDetails).hasSize(5);
        assertThat(allJobDetails.get(0)).hasClass(TestService.class).hasMethodName("doWork");
        Assertions.assertThat(allJobDetails.get(0).getJobParameters().get(0).getObject()).isEqualTo(workStream.get(0));
        Assertions.assertThat(allJobDetails.get(4).getJobParameters().get(0).getObject()).isEqualTo(workStream.get(4));
    }

    @Test
    void testIoCJobLambdaWithStream() {
        Stream<UUID> workStream = getWorkStream();
        AtomicInteger atomicInteger = new AtomicInteger();
        final IocJobLambdaFromStream<TestService, UUID> lambda = (service, uuid) -> service.doWork(uuid.toString(), atomicInteger.incrementAndGet(), now());
        final List<JobDetails> allJobDetails = workStream
                .map(x -> jobDetailsGenerator.toJobDetails(x, lambda))
                .collect(toList());

        Assertions.assertThat(allJobDetails).hasSize(5);
        assertThat(allJobDetails.get(0)).hasClass(TestService.class).hasMethodName("doWork");
        Assertions.assertThat(allJobDetails.get(0).getJobParameters().get(1).getObject()).isEqualTo(1);
        Assertions.assertThat(allJobDetails.get(4).getJobParameters().get(1).getObject()).isEqualTo(5);
    }

    @Test
    void testIoCJobWithStreamAndObject() {
        List<UUID> workStream = getWorkStream().collect(toUnmodifiableList());
        AtomicInteger atomicInteger = new AtomicInteger();
        final IocJobLambdaFromStream<TestService, TestService.Work> lambda = (service, work) -> service.doWork(work);
        final List<JobDetails> allJobDetails = workStream.stream()
                .map(uuid -> new TestService.Work(atomicInteger.getAndIncrement(), Integer.toString(atomicInteger.get()), uuid))
                .map(x -> jobDetailsGenerator.toJobDetails(x, lambda))
                .collect(toList());

        Assertions.assertThat(allJobDetails).hasSize(5);
        assertThat(allJobDetails.get(0)).hasClass(TestService.class).hasMethodName("doWork");
        Assertions.assertThat(allJobDetails.get(0).getJobParameters().get(0).getObject())
                .isInstanceOf(TestService.Work.class)
                .hasFieldOrPropertyWithValue("someString", "1")
                .hasFieldOrPropertyWithValue("uuid", workStream.get(0));
        Assertions.assertThat(allJobDetails.get(4).getJobParameters().get(0).getObject())
                .isInstanceOf(TestService.Work.class)
                .hasFieldOrPropertyWithValue("someString", "5")
                .hasFieldOrPropertyWithValue("uuid", workStream.get(4));
    }

    @Test
    void testIoCJobLambdaWithStaticMethodInLambda() {
        IocJobLambda<TestService> jobLambda = x -> x.doWork(TestService.Work.from(2, "a String", UUID.randomUUID()));
        final JobDetails jobDetails = toJobDetails(jobLambda);

        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWork");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        Assertions.assertThat(jobParameter.getClassName()).isEqualTo(TestService.Work.class.getName());
        Assertions.assertThat(jobParameter.getObject())
                .isInstanceOf(TestService.Work.class)
                .hasFieldOrPropertyWithValue("workCount", 2)
                .hasFieldOrPropertyWithValue("someString", "a String")
                .hasFieldOrProperty("uuid");
    }

    @Test
    void testInlineJobLambdaFromInterface() {
        JobDetails jobDetails = toJobDetails((JobLambda) () -> testServiceInterface.doWork());
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasNoArgs();
    }

    @Test
    void testMethodReferenceJobLambdaFromInterface() {
        JobDetails jobDetails = toJobDetails((JobLambda) testServiceInterface::doWork);
        assertThat(jobDetails)
                .hasClass(TestServiceInterface.class)
                .hasMethodName("doWork")
                .hasNoArgs();
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/335")
    void testJobLambdaWithDifferentParametersCalledFromOtherMethod() {
        UUID uuid1 = UUID.randomUUID();
        assertThat(createJobDetails(uuid1))
                .hasClass(TestService.GithubIssue335.class)
                .hasMethodName("run")
                .hasArgs(uuid1);

        UUID uuid2 = UUID.randomUUID();
        assertThat(createJobDetails(uuid2))
                .hasClass(TestService.GithubIssue335.class)
                .hasMethodName("run")
                .hasArgs(uuid2);
    }

    // must be kept in separate method for test of Github Issue 335
    private JobDetails createJobDetails(UUID uuid) {
        return toJobDetails(() -> new TestService.GithubIssue335().run(uuid));
    }

    private Stream<UUID> getWorkStream() {
        return IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID());
    }

    public void doWorkWithUUID(UUID uuid) {
        System.out.println("Doing some work... " + uuid);
    }

    public static class TestUtils {

        public static String getCurrentLogin() {
            return "Some string";
        }
    }
}
