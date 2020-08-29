package org.jobrunr.stubs;

/**
 * This class extends the TestService and it sole purpose is not to be registered in an IoC container and not to gave a default constructor.
 * This to test that the unhappy flow
 */
public class TestServiceThatCannotBeRun extends TestService {

    private final String anArgument;

    public TestServiceThatCannotBeRun(String anArgument) {
        this.anArgument = anArgument;
    }

    public String getAnArgument() {
        return anArgument;
    }
}
