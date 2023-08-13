package org.jobrunr.scheduling

import org.jobrunr.jobs.annotations.Job

class TestService {

    fun doWork(s: String) {
        println(s)
    }

    @Job(name = "Some neat Job Display Name")
    fun doWorkWithJobName(s: String, jobName: String) {
        println("$s $jobName")
    }

}