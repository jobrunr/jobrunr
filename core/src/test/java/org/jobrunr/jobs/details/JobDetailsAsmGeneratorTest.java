package org.jobrunr.jobs.details;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobContext;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.util.Textifier;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQResource;

public class JobDetailsAsmGeneratorTest {

    private TestService testService;
    private JobDetailsGenerator jobDetailsGenerator;

    @BeforeEach
    public void setUp() {
        jobDetailsGenerator = new JobDetailsAsmGenerator();
        testService = new TestService();
    }

    @Test
    @Disabled
    public void logByteCode() {
        String name = this.getClass().getName();
        String location = new File(".").getAbsolutePath() + "/build/classes/java/test/" + toFQResource(name) + ".class";
        assertThatCode(() -> Textifier.main(new String[]{location})).doesNotThrowAnyException();
    }

    @Test
    public void testJobLambdaCallingStaticMethod() {
        JobLambda job = () -> System.out.println("This is a test!");
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(System.class)
                .hasStaticFieldName("out")
                .hasMethodName("println")
                .hasArgs("This is a test!");
    }

    @Test
    public void testInlineJobLambdaCallInstanceMethod() {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails((JobLambda) () -> testService.doWork());
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasNoArgs();
    }

    @Test
    public void testJobLambdaCallInstanceMethod() {
        JobLambda job = () -> testService.doWork(5);
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(5);
    }

    @Test
    public void testJobLambdaWithIntegerAndJobContext() {
        JobLambda job = () -> testService.doWork(3, JobContext.Null);
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3, JobContext.Null);
    }

    @Test
    public void testJobLambdaWithDouble() {
        JobLambda job = () -> testService.doWork(3.3);
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3.3);
    }

    @Test
    public void testJobLambdaWithMultipleInts() {
        JobLambda job = () -> testService.doWork(3, 97693);
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3, 97693);
    }

    @Test
    public void testJobLambdaWithMultipleParameters() {
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            Instant now = Instant.now();
            JobLambda job = () -> testService.doWork("some string", finalI, now);

            JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
            assertThat(jobDetails)
                    .hasClass(TestService.class)
                    .hasMethodName("doWork")
                    .hasArgs("some string", finalI, now);
        }
    }

    @Test
    public void testJobLambdaWithObject() {
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            JobLambda job = () -> testService.doWork(new TestService.Work(finalI, "a String", UUID.randomUUID()));

            JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
            assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWork");
            JobParameter jobParameter = jobDetails.getJobParameters().get(0);
            assertThat(jobParameter.getClassName()).isEqualTo(TestService.Work.class.getName());
            assertThat(jobParameter.getObject())
                    .isInstanceOf(TestService.Work.class)
                    .hasFieldOrPropertyWithValue("workCount", finalI)
                    .hasFieldOrPropertyWithValue("someString", "a String")
                    .hasFieldOrProperty("uuid");
        }
    }

    @Test
    public void testJobLambdaWithFile() {
        JobLambda job = () -> testService.doWorkWithFile(new File("/tmp/file.txt"));

        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithFile");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        assertThat(jobParameter.getClassName()).isEqualTo(File.class.getName());
        assertThat(jobParameter.getObject())
                .isInstanceOf(File.class)
                .isEqualTo(new File("/tmp/file.txt"));
    }

    @Test
    public void testJobLambdaWithPath() {
        Path path = Paths.get("/tmp/file.txt");
        JobLambda job = () -> testService.doWorkWithPath(path);

        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithPath");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        assertThat(jobParameter.getClassName()).isEqualTo(Path.class.getName());
        assertThat(jobParameter.getObject())
                .isInstanceOf(Path.class)
                .isEqualTo(path);
    }

    @Test
    public void testJobLambdaWithPathsGetInLambda() {
        JobLambda job = () -> testService.doWorkWithPath(Paths.get("/tmp/file.txt"));

        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithPath");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        assertThat(jobParameter.getClassName()).isEqualTo(Path.class.getName());
        assertThat(jobParameter.getObject())
                .isInstanceOf(Path.class)
                .isEqualTo(Paths.get("/tmp/file.txt"));
    }

    @Test
    public void testJobLambdaWithPathsGetMultiplePartsInLambda() {
        JobLambda job = () -> testService.doWorkWithPath(Paths.get("/tmp", "folder", "subfolder", "file.txt"));

        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithPath");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        assertThat(jobParameter.getClassName()).isEqualTo(Path.class.getName());
        assertThat(jobParameter.getObject())
                .isInstanceOf(Path.class)
                .isEqualTo(Paths.get("/tmp/folder/subfolder/file.txt"));
    }

    @Test
    public void testJobLambdaWithPathOfInLambda() {
        JobLambda job = () -> testService.doWorkWithPath(Path.of("/tmp/file.txt"));

        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithPath");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        assertThat(jobParameter.getClassName()).isEqualTo(Path.class.getName());
        assertThat(jobParameter.getObject())
                .isInstanceOf(Path.class)
                .isEqualTo(Paths.get("/tmp/file.txt"));
    }

    @Test
    public void testJobLambdaWithObjectCreatedOutsideOfLambda() {
        for (int i = 0; i < 3; i++) {
            TestService.Work work = new TestService.Work(i, "a String", UUID.randomUUID());
            JobLambda job = () -> testService.doWork(work);

            JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
            assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWork");
            JobParameter jobParameter = jobDetails.getJobParameters().get(0);
            assertThat(jobParameter.getClassName()).isEqualTo(TestService.Work.class.getName());
            assertThat(jobParameter.getObject())
                    .isInstanceOf(TestService.Work.class)
                    .hasFieldOrPropertyWithValue("workCount", i)
                    .hasFieldOrPropertyWithValue("someString", "a String")
                    .hasFieldOrProperty("uuid");
        }
    }

    @Test
    public void testJobLambdaWithSupportedPrimitiveTypes() {
        JobLambda job = () -> testService.doWork(true, 3, 5L, 3.3F, 2.3D);
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(true, 3, 5L, 3.3F, 2.3D);
    }

    @Test
    public void testJobLambdaWithUnsupportedPrimitiveTypes() {
        JobLambda job = () -> testService.doWork((byte) 0x3, (short) 2, 'c');
        assertThatThrownBy(() -> jobDetailsGenerator.toJobDetails(job))
                .isInstanceOf(JobRunrException.class)
                .hasMessage("Error parsing lambda")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testJobLambdaCallingMultiLineStatement() {
        JobLambda job = () -> {
            UUID testId = UUID.randomUUID();
            int count = 6;
            LocalDateTime now = LocalDateTime.now();
            System.out.println("This is a test: " + testId + "; " + count + "; " + now);
        };
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        assertThat(jobDetails)
                .hasClass(System.class)
                .hasStaticFieldName("out")
                .hasMethodName("println")
                .hasArg(obj -> obj.toString().startsWith("This is a test: ")
                        && obj.toString().contains(" 6;")
                        && obj.toString().contains(LocalDateTime.now().withNano(0).toString()));
    }

    @Test
    public void testJobLambdaCallingMultiLineStatementThatLoadsFromAService() {
        JobLambda job = () -> {
            UUID testId = testService.getAnUUID();
            testService.doWork(testId);
        };

        assertThatThrownBy(() -> jobDetailsGenerator.toJobDetails(job))
                .isInstanceOf(JobRunrException.class)
                .hasMessage("Are you trying to enqueue a multiline lambda? This is not yet supported. If not, please create a bug report (if possible, provide the code to reproduce this and the stacktrace)");
    }

    @Test
    public void testJobLambdaWithArgumentThatIsNotUsed() {
        final Stream<Integer> range = IntStream.range(0, 1).boxed();
        assertThatCode(() -> jobDetailsGenerator.toJobDetails(range, (i) -> testService.doWork())).doesNotThrowAnyException();
    }

    @Test
    public void testIocJobLambda() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork();
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasNoArgs();
    }

    @Test
    public void testInlineIocJobLambda() {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails((IocJobLambda<TestService>) (x) -> x.doWork(5));
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(5);
    }

    @Test
    public void testIocJobLambdaWithIntegerAndJobContext() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(3, JobContext.Null);
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3, JobContext.Null);
    }

    @Test
    public void testIocJobLambdaWithDouble() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(3.3);
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3.3);
    }

    @Test
    public void testIocJobLambdaWithMultipleInts() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(3, 97693);
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(3, 97693);
    }

    @Test
    public void testIocJobLambdaWithMultipleParameters() {
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            Instant now = Instant.now();
            IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork("some string", finalI, now);

            JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJobLambda);
            assertThat(jobDetails)
                    .hasClass(TestService.class)
                    .hasMethodName("doWork")
                    .hasArgs("some string", finalI, now);
        }
    }

    @Test
    public void testIocJobLambdaWithObject() {
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(new TestService.Work(finalI, "a String", UUID.randomUUID()));

            JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJobLambda);
            assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWork");
            JobParameter jobParameter = jobDetails.getJobParameters().get(0);
            assertThat(jobParameter.getClassName()).isEqualTo(TestService.Work.class.getName());
            assertThat(jobParameter.getObject())
                    .isInstanceOf(TestService.Work.class)
                    .hasFieldOrPropertyWithValue("workCount", finalI)
                    .hasFieldOrPropertyWithValue("someString", "a String")
                    .hasFieldOrProperty("uuid");
        }
    }

    @Test
    public void testIoCJobLambdaWithFile() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWorkWithFile(new File("/tmp/file.txt"));

        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJobLambda);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithFile");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        assertThat(jobParameter.getClassName()).isEqualTo(File.class.getName());
        assertThat(jobParameter.getObject())
                .isInstanceOf(File.class)
                .isEqualTo(new File("/tmp/file.txt"));
    }

    @Test
    public void testIoCJobLambdaWithPath() {
        Path path = Paths.get("/tmp/file.txt");
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWorkWithPath(path);

        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJobLambda);
        assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWorkWithPath");
        JobParameter jobParameter = jobDetails.getJobParameters().get(0);
        assertThat(jobParameter.getClassName()).isEqualTo(Path.class.getName());
        assertThat(jobParameter.getObject())
                .isInstanceOf(Path.class)
                .isEqualTo(path);
    }

    @Test
    public void testIocJobLambdaWithObjectCreatedOutsideOfLambda() {
        for (int i = 0; i < 3; i++) {
            TestService.Work work = new TestService.Work(i, "a String", UUID.randomUUID());
            IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(work);

            JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJobLambda);
            assertThat(jobDetails).hasClass(TestService.class).hasMethodName("doWork");
            JobParameter jobParameter = jobDetails.getJobParameters().get(0);
            assertThat(jobParameter.getClassName()).isEqualTo(TestService.Work.class.getName());
            assertThat(jobParameter.getObject())
                    .isInstanceOf(TestService.Work.class)
                    .hasFieldOrPropertyWithValue("workCount", i)
                    .hasFieldOrPropertyWithValue("someString", "a String")
                    .hasFieldOrProperty("uuid");
        }
    }

    @Test
    public void testIocJobLambdaWithSupportedPrimitiveTypes() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(true, 3, 5L, 3.3F, 2.3D);
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJobLambda);
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(true, 3, 5L, 3.3F, 2.3D);
    }

    @Test
    public void testIocJobLambdaWithUnsupportedPrimitiveTypes() {
        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork((byte) 0x3, (short) 2, 'c');
        assertThatThrownBy(() -> jobDetailsGenerator.toJobDetails(iocJobLambda))
                .isInstanceOf(JobRunrException.class)
                .hasMessage("Error parsing lambda")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIocJobLambdaWithArgumentThatIsNotUsed() {
        IocJobLambdaFromStream<TestService, Integer> iocJobLambdaFromStream = (x, i) -> x.doWork();
        assertThatCode(() -> jobDetailsGenerator.toJobDetails(5, iocJobLambdaFromStream)).doesNotThrowAnyException();
    }

}