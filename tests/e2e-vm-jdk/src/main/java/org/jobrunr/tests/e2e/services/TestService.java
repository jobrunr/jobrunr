package org.jobrunr.tests.e2e.services;

public class TestService {

    public void doWork() {
        System.out.println("This is a test service");
    }

    public void doWork(Work work) {
        System.out.println("This is a test service " + work.getSomeId() + "; " + work.getSomeString() + "; " + work.getSomeInt() + "; " + work.getSomeLong());
    }

}
