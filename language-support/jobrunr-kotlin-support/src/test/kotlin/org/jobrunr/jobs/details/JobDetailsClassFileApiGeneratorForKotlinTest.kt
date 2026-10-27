package org.jobrunr.jobs.details

import org.assertj.core.api.Assumptions.assumeThat
import org.jobrunr.JobRunrAssertions.assertThat
import org.jobrunr.jobs.details.lambdas.AbstractJobDetailsGeneratorForKotlinTest
import org.jobrunr.utils.VersionNumber
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Kotlin lambdas resolved by the Class File API backend. The backend is reached through the factory, as a
 * user would: kotlinc does not resolve the META-INF/versions/25 classes of the core jar, so the class
 * itself is not referenceable from here.
 */
class JobDetailsClassFileApiGeneratorForKotlinTest : AbstractJobDetailsGeneratorForKotlinTest() {

    @BeforeEach
    fun onlyOnJdk25AndUp() {
        assumeThat(VersionNumber.JAVA_VERSION.hasMajorVersionHigherOrEqualTo(25))
            .withFailMessage("java.lang.classfile needs JDK 25 or higher")
            .isTrue()
    }

    override fun jobDetailsGenerator(): JobDetailsGenerator = JobDetailsGeneratorFactory.createDefault()

    @Test
    fun theFactorySelectsTheClassFileApiGenerator() {
        assertThat(jobDetailsGenerator()::class.java.name).isEqualTo("org.jobrunr.jobs.details.JobDetailsClassFileApiGenerator")
    }
}
