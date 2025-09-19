package org.jobrunr.jobs.details

import io.github.artsok.RepeatedIfExceptionsTest
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jobrunr.JobRunrAssertions.assertThat
import org.jobrunr.JobRunrException
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.context.JobContext
import org.jobrunr.jobs.lambdas.IocJobLambda
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream
import org.jobrunr.jobs.lambdas.JobLambda
import org.jobrunr.jobs.lambdas.JobLambdaFromStream
import org.jobrunr.stubs.TestService
import org.jobrunr.stubs.TestServiceInterface
import org.jobrunr.utils.StringUtils.substringBeforeLast
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class JobDetailsAsmGeneratorForKotlinTest {

    private val testService = TestService()
    private val testServiceInterface: TestServiceInterface = testService
    val jobDetailsGenerator = JobDetailsAsmGenerator()

    private enum class SomeEnum {
        Value1, Value2
    }

    @Test
    fun testMultipleJobsShowsNiceException() {
        assertThatThrownBy {
            toJobDetails {
                doWorkWithUUID()
                doWorkWithUUID()
            }
        }
            .isInstanceOf(JobRunrException::class.java)
            .hasMessage("JobRunr only supports enqueueing/scheduling of one method")
    }

    @Test
    fun testJobLambdaCallingStaticMethod() {
        val jobDetails = toJobDetails { println("This is a test!") }
        assertThat(jobDetails)
            .hasClass(System::class.java)
            .hasStaticFieldName("out")
            .hasMethodName("println")
            .hasArgs("This is a test!")
    }

    @Test
    fun testJobLambdaCallMethodReference() {
        val jobDetails = toJobDetails { testService::doWorkWithoutParameters }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWorkWithoutParameters")
            .hasNoArgs()
    }

    @Test
    fun testJobLambdaCallMethodReferenceInlineService() {
        val service = TestService()
        val jobDetails = toJobDetails { service::doWorkWithoutParameters }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWorkWithoutParameters")
            .hasNoArgs()
    }

    @Test
    fun testJobLambdaCallInstanceMethod_Null() {
        val work: TestService.Work? = null
        val jobDetails = toJobDetails { testService.doWork(work) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArg(Objects::isNull)
    }

    @Test
    fun testJobLambdaCallInstanceMethodWithObject() {
        val uuid = UUID.randomUUID()
        val work: TestService.Work = TestService.Work(5, "string", uuid)
        val jobDetails = toJobDetails { testService.doWork(work) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArgs(work)
    }

    @Test
    fun testJobLambdaCallInstanceMethod_BIPUSH() {
        val jobDetails = toJobDetails { testService.doWork(5) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArgs(5)
    }

    @Test
    fun testJobLambdaCallInstanceMethod_SIPUSH() {
        val jobDetails = toJobDetails { testService.doWorkThatTakesLong(500) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWorkThatTakesLong")
            .hasArgs(500)
    }

    @Test
    fun testJobLambdaCallInstanceMethod_LCONST() {
        val jobDetails = toJobDetails { testService.doWorkThatTakesLong(1L) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWorkThatTakesLong")
            .hasArgs(1L)
    }

    @Test
    fun testInlineJobLambdaCallInstanceMethod() {
        val jobDetails = toJobDetails { testService.doWork() }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasNoArgs()
    }

    @Test
    fun testJobLambdaWithIntegerAndJobContext() {
        val jobDetails = toJobDetails { testService.doWork(3, JobContext.Null) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArgs(3, JobContext.Null)
    }

    @Test
    fun testJobLambdaWithDouble() {
        val jobDetails = toJobDetails { testService.doWork(3.3) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArgs(3.3)
    }

    @Test
    fun testJobLambdaWithMultipleInts() {
        val jobDetails = toJobDetails { testService.doWork(3, 97693) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArgs(3, 97693)
    }

    @Test
    fun testJobLambdaWithMultipleParameters() {
        for (i in 0..2) {
            val now = Instant.now()
            val jobDetails = toJobDetails { testService.doWork("some string", i, now) }
            assertThat(jobDetails)
                .hasClass(TestService::class.java)
                .hasMethodName("doWork")
                .hasArgs("some string", i, now)
        }
    }

    @Test
    fun testJobLambdaWithObject() {
        for (i in 0..2) {
            val jobDetails = toJobDetails { testService.doWork(TestService.Work(i, "a String", UUID.randomUUID())) }
            assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWork")
            val jobParameter = jobDetails.jobParameters[0]
            assertThat(jobParameter.className).isEqualTo(TestService.Work::class.java.name)
            assertThat(jobParameter.getObject())
                .isInstanceOf(TestService.Work::class.java)
                .hasFieldOrPropertyWithValue("workCount", i)
                .hasFieldOrPropertyWithValue("someString", "a String")
                .hasFieldOrProperty("uuid")
        }
    }

    @Test
    fun testJobLambdaWithFile() {
        val jobDetails = toJobDetails { testService.doWorkWithFile(File("/tmp/file.txt")) }
        assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWorkWithFile")
        val jobParameter = jobDetails.jobParameters[0]
        assertThat(jobParameter.className).isEqualTo(File::class.java.name)
        assertThat(jobParameter.getObject())
            .isInstanceOf(File::class.java)
            .isEqualTo(File("/tmp/file.txt"))
    }

    @Test
    fun testJobLambdaWithPath() {
        val path = Paths.get("/tmp/file.txt")
        val jobDetails = toJobDetails { testService.doWorkWithPath(path) }
        assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWorkWithPath")
        val jobParameter = jobDetails.jobParameters[0]
        assertThat(jobParameter.className).isEqualTo(Path::class.java.name)
        assertThat(jobParameter.getObject())
            .isInstanceOf(Path::class.java)
            .isEqualTo(path)
    }

    @Test
    fun testJobLambdaWithPathsGetInLambda() {
        val jobDetails = toJobDetails { testService.doWorkWithPath(Paths.get("/tmp/file.txt")) }
        assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWorkWithPath")
        val jobParameter = jobDetails.jobParameters[0]
        assertThat(jobParameter.className).isEqualTo(Path::class.java.name)
        assertThat(jobParameter.getObject())
            .isInstanceOf(Path::class.java)
            .isEqualTo(Paths.get("/tmp/file.txt"))
    }

    @Test
    fun testJobLambdaWithPathsGetMultiplePartsInLambda() {
        val jobDetails = toJobDetails { testService.doWorkWithPath(Paths.get("/tmp", "folder", "subfolder", "file.txt")) }
        assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWorkWithPath")
        val jobParameter = jobDetails.jobParameters[0]
        assertThat(jobParameter.className).isEqualTo(Path::class.java.name)
        assertThat(jobParameter.getObject())
            .isInstanceOf(Path::class.java)
            .isEqualTo(Paths.get("/tmp/folder/subfolder/file.txt"))
    }

    @Test
    fun testJobLambdaWithPathOfInLambda() {
        val jobDetails = toJobDetails { testService.doWorkWithPath(Paths.get("/tmp/file.txt")) }
        assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWorkWithPath")
        val jobParameter = jobDetails.jobParameters[0]
        assertThat(jobParameter.className).isEqualTo(Path::class.java.name)
        assertThat(jobParameter.getObject())
            .isInstanceOf(Path::class.java)
            .isEqualTo(Paths.get("/tmp/file.txt"))
    }

    @Test
    fun testJobLambdaWithObjectCreatedOutsideOfLambda() {
        for (i in 0..2) {
            val work = TestService.Work(i, "a String", UUID.randomUUID())
            val job = JobLambda { testService.doWork(work) }
            val jobDetails = jobDetailsGenerator.toJobDetails(job)
            assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWork")
            val jobParameter = jobDetails.jobParameters[0]
            assertThat(jobParameter.className).isEqualTo(TestService.Work::class.java.name)
            assertThat(jobParameter.getObject())
                .isInstanceOf(TestService.Work::class.java)
                .hasFieldOrPropertyWithValue("workCount", i)
                .hasFieldOrPropertyWithValue("someString", "a String")
                .hasFieldOrProperty("uuid")
        }
    }

    @Test
    fun testJobLambdaWithSupportedPrimitiveTypes() {
        val jobDetails = toJobDetails { testService.doWork(true, 3, 5L, 3.3f, 2.3) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArgs(true, 3, 5L, 3.3f, 2.3)
    }

    @Test
    fun testJobLambdaWithUnsupportedPrimitiveTypes() {
        assertThatThrownBy { toJobDetails { testService.doWork(0x3.toByte(), 2.toShort(), 'c') } }
            .isInstanceOf(JobRunrException::class.java)
            .hasMessage("Error parsing lambda")
            .hasCauseInstanceOf(IllegalArgumentException::class.java)
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    fun testJobLambdaCallingMultiLineStatement() {
        val jobDetails = toJobDetails {
            val testId = UUID.randomUUID()
            val someInt = 6
            val someDouble = 5.3
            val someFloat = 5.3f
            val someLong = 3L
            val someBoolean = true
            val someEnum: SomeEnum = SomeEnum.Value1
            val now = LocalDateTime.now()
            println("This is a test: $testId; $someInt; $someDouble; $someFloat; $someLong; $someBoolean; $someEnum; $now")
        }
        assertThat(jobDetails)
            .hasClass(System::class.java)
            .hasStaticFieldName("out")
            .hasMethodName("println")
            .hasArg { obj: Any ->
                (obj.toString().startsWith("This is a test: ")
                        && obj.toString().contains(" 6; 5.3; 5.3; 3; true; Value1;")
                        && obj.toString().contains(substringBeforeLast(LocalDateTime.now().toString(), ":")))
            }
    }

    @Test
    fun testJobLambdaCallingMultiLineStatementThatLoadsFromAService() {
        val jobDetails = toJobDetails {
            val testId = testService.anUUID
            testService.doWork(testId)
        }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArg { obj: Any? -> obj is UUID }
    }

    @Test
    fun testJobLambdaWithArgumentThatIsNotUsed() {
        val range = (0..1).asSequence()
        assertThatCode { toJobDetails(range) { _ -> testService.doWork() } }.doesNotThrowAnyException()
    }

    @Test
    fun testJobLambdaWithStaticMethodInLambda() {
        val jobDetails = toJobDetails { testService.doWork(TestService.Work.from(2, "a String", UUID.randomUUID())) }
        assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWork")
        val jobParameter = jobDetails.jobParameters[0]
        assertThat(jobParameter.className).isEqualTo(TestService.Work::class.java.name)
        assertThat(jobParameter.getObject())
            .isInstanceOf(TestService.Work::class.java)
            .hasFieldOrPropertyWithValue("workCount", 2)
            .hasFieldOrPropertyWithValue("someString", "a String")
            .hasFieldOrProperty("uuid")
    }

    @Test
    fun testJobLambdaWhichReturnsSomething() {
        val jobDetails = toJobDetails { testService.doWorkAndReturnResult("someString") }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWorkAndReturnResult")
            .hasArgs("someString")
    }

    @Test
    fun testJobLambdaWithStream() {
        val workStream: Sequence<UUID> = workSequence()
        val atomicInteger = AtomicInteger()
        val allJobDetails =
            toJobDetails(workStream) { uuid: UUID -> testService.doWork(uuid.toString(), atomicInteger.incrementAndGet(), Instant.now()) }.toList()
        assertThat(allJobDetails).hasSize(5)
        assertThat(allJobDetails[0]).hasClass(TestService::class.java).hasMethodName("doWork")
        assertThat(allJobDetails[0].jobParameters[1].getObject()).isEqualTo(1)
        assertThat(allJobDetails[4].jobParameters[1].getObject()).isEqualTo(5)
    }

    @Test
    fun testJobLambdaWithStreamAndMethodReference() {
        val workStream: Sequence<UUID> = workSequence()
        val allJobDetails = toJobDetails(workStream) { obj: TestService, uuid: UUID? -> obj.doWorkWithUUID(uuid) }.toList()
        assertThat(allJobDetails).hasSize(5)
        assertThat(allJobDetails[0])
            .hasClass(TestService::class.java)
            .hasMethodName("doWorkWithUUID")
            .hasArgs(workStream.first())
    }

    @Test
    fun testJobLambdaWithMethodInSameFile() {
        val uuid = UUID.randomUUID()
        val jobDetails: JobDetails = toJobDetails { doWorkWithUUID(uuid) }
        assertThat(jobDetails)
            .hasClass(JobDetailsAsmGeneratorForKotlinTest::class.java)
            .hasMethodName("doWorkWithUUID")
            .hasArgs(uuid)
    }

    @Test
    fun testIocJobLambda() {
        val jobDetails = toJobDetails<TestService> { it.doWork() }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasNoArgs()
    }

    @Test
    fun testInlineIocJobLambda() {
        val jobDetails = toJobDetails<TestService> { it.doWork(5) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArgs(5)
    }

    @Test
    fun testIocJobLambdaWithIntegerAndJobContext() {
        val jobDetails = toJobDetails<TestService> { it.doWork(3, JobContext.Null) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArgs(3, JobContext.Null)
    }

    @Test
    fun testIocJobLambdaWithDouble() {
        val jobDetails = toJobDetails<TestService> { it.doWork(3456.3) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArgs(3456.3)
    }

    @Test
    fun testIocJobLambdaWithMultipleInts() {
        val jobDetails = toJobDetails<TestService> { it.doWork(3, 97693) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArgs(3, 97693)
    }

    @Test
    fun testIocJobLambdaWithMultipleParameters() {
        for (i in 0..2) {
            val now = Instant.now()
            val jobDetails = toJobDetails<TestService> { it.doWork("some string", i, now) }
            assertThat(jobDetails)
                .hasClass(TestService::class.java)
                .hasMethodName("doWork")
                .hasArgs("some string", i, now)
        }
    }

    @Test
    fun testIocJobLambdaWithObject() {
        for (i in 0..2) {
            val jobDetails = toJobDetails<TestService> { it.doWork(TestService.Work(i, "a String", UUID.randomUUID())) }
            assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWork")
            val jobParameter = jobDetails.jobParameters[0]
            assertThat(jobParameter.className).isEqualTo(TestService.Work::class.java.name)
            assertThat(jobParameter.getObject())
                .isInstanceOf(TestService.Work::class.java)
                .hasFieldOrPropertyWithValue("workCount", i)
                .hasFieldOrPropertyWithValue("someString", "a String")
                .hasFieldOrProperty("uuid")
        }
    }

    @Test
    fun testIoCJobLambdaWithFile() {
        val jobDetails = toJobDetails<TestService> { it.doWorkWithFile(File("/tmp/file.txt")) }
        assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWorkWithFile")
        val jobParameter = jobDetails.jobParameters[0]
        assertThat(jobParameter.className).isEqualTo(File::class.java.name)
        assertThat(jobParameter.getObject())
            .isInstanceOf(File::class.java)
            .isEqualTo(File("/tmp/file.txt"))
    }

    @Test
    fun testIoCJobLambdaWithPath() {
        val path = Paths.get("/tmp/file.txt")
        val jobDetails = toJobDetails<TestService> { it.doWorkWithPath(path) }
        assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWorkWithPath")
        val jobParameter = jobDetails.jobParameters[0]
        assertThat(jobParameter.className).isEqualTo(Path::class.java.name)
        assertThat(jobParameter.getObject())
            .isInstanceOf(Path::class.java)
            .isEqualTo(path)
    }

    @Test
    fun testIocJobLambdaWithObjectCreatedOutsideOfLambda() {
        for (i in 0..2) {
            val work = TestService.Work(i, "a String", UUID.randomUUID())
            val jobDetails = toJobDetails<TestService> { it.doWork(work) }
            assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWork")
            val jobParameter = jobDetails.jobParameters[0]
            assertThat(jobParameter.className).isEqualTo(TestService.Work::class.java.name)
            assertThat(jobParameter.getObject())
                .isInstanceOf(TestService.Work::class.java)
                .hasFieldOrPropertyWithValue("workCount", i)
                .hasFieldOrPropertyWithValue("someString", "a String")
                .hasFieldOrProperty("uuid")
        }
    }

    @Test
    fun testIocJobLambdaWithSupportedPrimitiveTypes() {
        val jobDetails = toJobDetails<TestService> { it.doWork(true, 3, 5L, 3.3f, 2.3) }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasArgs(true, 3, 5L, 3.3f, 2.3)
    }

    @Test
    fun testIocJobLambdaWithSupportedPrimitiveTypes_LOAD() {
        for (i in 0..2) {
            val finalB = i % 2 == 0
            val finalL = 5L + i
            val finalF = 3.3f + i
            val finalD = 2.3 + i
            val jobDetails = toJobDetails<TestService> { it.doWork(finalB, i, finalL, finalF, finalD) }
            assertThat(jobDetails)
                .hasClass(TestService::class.java)
                .hasMethodName("doWork")
                .hasArgs(i % 2 == 0, i, 5L + i, 3.3f + i, 2.3 + i)
        }
    }

    @Test
    fun testIocJobLambdaWithUnsupportedPrimitiveTypes() {
        assertThatThrownBy { toJobDetails<TestService> { it.doWork(0x3.toByte(), 2.toShort(), 'c') } }
            .isInstanceOf(JobRunrException::class.java)
            .hasMessage("Error parsing lambda")
            .hasCauseInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun testIocJobLambdaWithArgumentThatIsNotUsed() {
        assertThatCode { jobDetailsGenerator.toJobDetails(5, { x: TestService, i: Int? -> x.doWork() }) }.doesNotThrowAnyException()
        val jobDetails = jobDetailsGenerator.toJobDetails(5, { x: TestService, i: Int? -> x.doWork() })
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasNoArgs()
    }

    @Test
    fun testIocJobLambdaWhichReturnsSomething() {
        val jobDetails = toJobDetails<TestService> { it.doWorkAndReturnResult("someString") }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWorkAndReturnResult")
            .hasArgs("someString")
    }

    @Test
    fun testIoCJobLambdaWithStream() {
        val workSequence: Sequence<UUID> = workSequence()
        val atomicInteger = AtomicInteger()

        val allJobDetails = toJobDetails<TestService, UUID>(workSequence) { service: TestService, uuid: UUID ->
            service.doWork(
                uuid.toString(),
                atomicInteger.incrementAndGet(),
                Instant.now()
            )
        }.toList()
        assertThat(allJobDetails).hasSize(5)
        assertThat(allJobDetails[0]).hasClass(TestService::class.java).hasMethodName("doWork")
        assertThat(allJobDetails[0].jobParameters[1].getObject()).isEqualTo(1)
        assertThat(allJobDetails[4].jobParameters[1].getObject()).isEqualTo(5)
    }

    @Test
    fun testIoCJobLambdaWithStaticMethodInLambda() {
        val jobDetails = toJobDetails<TestService> { it.doWork(TestService.Work.from(2, "a String", UUID.randomUUID())) }
        assertThat(jobDetails).hasClass(TestService::class.java).hasMethodName("doWork")
        val jobParameter = jobDetails.jobParameters[0]
        assertThat(jobParameter.className).isEqualTo(TestService.Work::class.java.name)
        assertThat(jobParameter.getObject())
            .isInstanceOf(TestService.Work::class.java)
            .hasFieldOrPropertyWithValue("workCount", 2)
            .hasFieldOrPropertyWithValue("someString", "a String")
            .hasFieldOrProperty("uuid")
    }

    @Test
    fun testInlineJobLambdaFromInterface() {
        val jobDetails = toJobDetails { testServiceInterface.doWork() }
        assertThat(jobDetails)
            .hasClass(TestService::class.java)
            .hasMethodName("doWork")
            .hasNoArgs()
    }

    @Test
    fun testMethodReferenceJobLambdaFromInterface() {
        val jobDetails = toJobDetails { testServiceInterface::doWork }
        assertThat(jobDetails)
            .hasClass(TestServiceInterface::class.java)
            .hasMethodName("doWork")
            .hasNoArgs()
    }

    @Test
    fun testMethodReferenceJobLambdaInSameClass() {
        val jobDetails = toJobDetails(::doWorkWithUUID)
        assertThat(jobDetails)
            .hasClass(JobDetailsAsmGeneratorForKotlinTest::class.java)
            .hasMethodName("doWorkWithUUID")
            .hasNoArgs()
    }

    fun bla(job: JobLambda) {
        job.run()
    }

    fun toJobDetails(job: JobLambda): JobDetails {
        return jobDetailsGenerator.toJobDetails(job)
    }

    inline fun <reified S> toJobDetails(job: IocJobLambda<S>): JobDetails {
        return jobDetailsGenerator.toJobDetails(job)
    }

    fun <T> toJobDetails(input: Sequence<T>, jobFromStream: JobLambdaFromStream<T>): Sequence<JobDetails> {
        return input
            .map { x -> jobDetailsGenerator.toJobDetails(x, jobFromStream) }
    }

    inline fun <reified S, T> toJobDetails(input: Sequence<T>, jobFromStream: IocJobLambdaFromStream<S, T>): Sequence<JobDetails> {
        return input
            .map { x -> jobDetailsGenerator.toJobDetails(x, jobFromStream) }
    }

    fun workSequence(): Sequence<UUID> {
        return (0..4)
            .map { UUID.randomUUID() }
            .asSequence()
    }

    fun doWorkWithUUID(uuid: UUID) {
        println("Doing some work... $uuid")
    }

    fun doWorkWithUUID() {
        println("Doing some work without parameters")
    }

}