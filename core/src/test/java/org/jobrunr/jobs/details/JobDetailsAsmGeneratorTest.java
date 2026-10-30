package org.jobrunr.jobs.details;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.util.Textifier;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQResource;

class JobDetailsAsmGeneratorTest extends AbstractJobDetailsGeneratorTest {

    @Override
    protected JobDetailsGenerator getJobDetailsGenerator() {
        return new JobDetailsAsmGenerator();
    }

    @Test
    @Disabled("for debugging")
    void logByteCode() {
        String name = AbstractJobDetailsGeneratorTest.class.getName();
        String location = new File(".").getAbsolutePath() + "/build/classes/java/testFixtures/" + toFQResource(name) + ".class";

        assertThatCode(() -> Textifier.main(new String[]{location})).doesNotThrowAnyException();
    }
}
