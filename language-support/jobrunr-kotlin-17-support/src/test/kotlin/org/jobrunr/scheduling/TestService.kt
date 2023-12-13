package org.jobrunr.scheduling

import org.jobrunr.jobs.annotations.Job
import java.util.*

class TestService {

    fun doWork(s: String) {
        println(s)
    }

    fun doWorkWithDefaultParameter(id: UUID = UUID.randomUUID()) {
        println("id: $id")
    }

    @Job(name = "Some neat Job Display Name")
    fun doWorkWithJobName(s: String, jobName: String) {
        println("$s $jobName")
    }

}