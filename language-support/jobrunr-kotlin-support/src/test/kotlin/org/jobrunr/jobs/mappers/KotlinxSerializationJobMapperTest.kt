package org.jobrunr.jobs.mappers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThat
import org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob
import org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper
import org.jobrunr.utils.mapper.JsonMapper
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper
import org.junit.jupiter.api.Test

class KotlinxSerializationJobMapperTest : JobMapperTest() {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override fun getJsonMapper(): JsonMapper {
        return KotlinxSerializationJsonMapper(testModule)
    }

    @Test
    fun `serialize job metadata with a Kotlin-specific custom class`() {
        val job = anEnqueuedJob()
            .withMetadata("custom kotlin class", MyCustomKotlinMetadataClass("some name"))
            .build()

        val jobAsJson1 = jobMapper.serializeJob(job)
        val actualJob1 = jobMapper.deserializeJob(jobAsJson1)
        assertThat(actualJob1.metadata).isEqualTo(job.metadata)
    }

    @Test
    fun `serialize job metadata with arrays`() {
        val job = anEnqueuedJob()
            .withMetadata("some kotlin array with int types", intArrayOf(1, 2, 3))
            .build()

        val jobAsJson1 = jobMapper.serializeJob(job)
        val actualJob1 = jobMapper.deserializeJob(jobAsJson1)
        assertThat(actualJob1.metadata).usingRecursiveComparison().isEqualTo(job.metadata)
    }

    @Test
    fun `serialize job metadata with arrays from jackson to kotlinx`() {
        val kotlinMapper = jobMapper
        val jacksonMapper = JobMapper(JacksonJsonMapper())
        val job = anEnqueuedJob()
            .withMetadata("some kotlin array with int types", intArrayOf(1, 2, 3))
            .build()

        val jobAsJson1 = jacksonMapper.serializeJob(job)

        val actualJob1 = kotlinMapper.deserializeJob(jobAsJson1)
        assertThat(actualJob1.metadata).usingRecursiveComparison().isEqualTo(job.metadata)
    }

    @Test
    fun `serialize job metadata with arrays from kotlinx to jackson`() {
        val kotlinMapper = jobMapper
        val jacksonMapper = JobMapper(JacksonJsonMapper())
        val job = anEnqueuedJob()
            .withMetadata("some kotlin array with int types", intArrayOf(1, 2, 3))
            .build()

        val jobAsJson = kotlinMapper.serializeJob(job)

        val actualJob1 = jacksonMapper.deserializeJob(jobAsJson)
        assertThat(actualJob1.metadata).usingRecursiveComparison().isEqualTo(job.metadata)
    }

    @Serializable
    class MyCustomKotlinMetadataClass(val name: String) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MyCustomKotlinMetadataClass

            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }
}
