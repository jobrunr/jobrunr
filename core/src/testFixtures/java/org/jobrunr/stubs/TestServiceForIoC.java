package org.jobrunr.stubs;

/**
 * This class extends the TestService and it sole purpose is not to have a default constructor.
 * This to test that the service is injected by the IoC framework and can not be instantiated directly
 */
public class TestServiceForIoC extends TestService {

    private final String anArgument;

    public TestServiceForIoC(String anArgument) {
        this.anArgument = anArgument;
    }

    public String getAnArgument() {
        return anArgument;
    }
}
