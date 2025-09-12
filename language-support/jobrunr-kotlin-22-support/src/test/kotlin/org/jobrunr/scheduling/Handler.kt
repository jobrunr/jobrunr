package org.jobrunr.scheduling

import kotlinx.coroutines.runBlocking
import org.jobrunr.jobs.lambdas.JobRequestHandler as JobRunrRequestHandler
import java.util.*
import org.jobrunr.jobs.lambdas.JobRequest as JobRunrRequest
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jobrunr.scheduling.JobRequestScheduler as JobRunrJobRequestScheduler

interface JobRequestHandler<T: JobRequest>: JobRunrRequestHandler<T> {
    suspend fun runSuspend(jobRequest: T)

    override fun run(jobRequest: T) {
        runBlocking {
            runSuspend(jobRequest)
        }
    }
}

interface JobRequest: JobRunrRequest {
    val id: UUID
    val jobName: String
}

@Serializable
data class ExampleJobRequest(
    val something: String,
): JobRequest {
    @Contextual
    override val id: UUID = UUID.randomUUID()
    override val jobName: String = ExampleJobRequest::class.simpleName!!

    override fun getJobRequestHandler(): Class<ExampleJobRequestHandler> {
        return ExampleJobRequestHandler::class.java
    }
}

class ExampleJobRequestHandler(
    private val exampleService: ExampleService,
): JobRequestHandler<ExampleJobRequest> {
    override suspend fun runSuspend(jobRequest: ExampleJobRequest) {
        exampleService.doSomething(jobRequest)
    }
}

class ExampleService {
    fun doSomething(jobRequest: ExampleJobRequest) {
        println("Something: ${jobRequest.something}")
    }
}


class JobService(
    private val jobRequestScheduler: JobRunrJobRequestScheduler,
) {
    suspend fun startJob(jobRequest: JobRequest): UUID {
        return jobRequestScheduler.enqueue(jobRequest.id, jobRequest).asUUID()
    }
}