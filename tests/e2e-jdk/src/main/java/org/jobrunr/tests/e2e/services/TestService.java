package org.jobrunr.tests.e2e.services;

import java.util.UUID;

public class TestService {

    public void doWork() {
        System.out.println("This is a test service");
    }

    public void doWork(UUID id) {
        System.out.println("This is a test service " + id.toString());
    }

}
