package org.jobrunr.tests.e2e.services;

import java.util.UUID;

public class Work {

    private int someInt;
    private String someString;
    private Long someLong;
    private UUID someId;

    public Work(int someInt, String someString, Long someLong, UUID someId) {
        this.someInt = someInt;
        this.someString = someString;
        this.someLong = someLong;
        this.someId = someId;
    }

    protected Work() {
    }

    public int getSomeInt() {
        return someInt;
    }

    public String getSomeString() {
        return someString;
    }

    public Long getSomeLong() {
        return someLong;
    }

    public UUID getSomeId() {
        return someId;
    }
}
