package org.jobrunr.scheduling

import org.jobrunr.jobs.annotations.Job
import java.util.*

class TestService {

    data class MyStateObject(val name: String, val age: Int)

    fun doWork(s: String) {
        println(s)
    }

    fun doWorkWithDefaultParameter(id: UUID = UUID.randomUUID()) {
        println("id: $id")
    }

    fun doWorkWithPair(s: Pair<String, String>) {
        println("Pair: " + s.first + "; " + s.second)
    }

    fun doWorkWithDataClass(mso: MyStateObject) {
        println("Pair: " + mso.name + "; " + mso.age)
    }

    @Job(name = "Some neat Job Display Name")
    fun doWorkWithJobName(s: String, jobName: String) {
        println("$s $jobName")
    }

    @Job(name = "Do something with ids", retries = 3)
    fun doSomethingWithIds(someId: Long, anotherId: Long) {
        print("having fun with ids $someId, $anotherId")
    }

}