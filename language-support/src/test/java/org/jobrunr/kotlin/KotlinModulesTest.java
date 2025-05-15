package org.jobrunr.kotlin;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.StringUtils.substringAfterLast;
import static org.jobrunr.utils.StringUtils.substringBetween;

public class KotlinModulesTest {

    @Test
    void testAllKotlinModules() throws IOException {
        File file = new File(".");
        String[] allKotlinSubModules = file.list((dir, name) -> name.startsWith("jobrunr-kotlin-"));
        for (String kotlinSupportModule : allKotlinSubModules) {
            String kotlinDirVersion = substringBetween(kotlinSupportModule, "jobrunr-kotlin-", "-support");
            String kotlinVersion = String.join(".", kotlinDirVersion.split("|"));
            checkGradleFile(kotlinSupportModule, kotlinVersion);
            checkKoinJobActivator(kotlinSupportModule);
            checkKotlinSerializationPresent(kotlinSupportModule);
        }
    }

    private void checkGradleFile(String kotlinSupportModule, String kotlinVersion) throws IOException {
        Path path = Paths.get(kotlinSupportModule, "build.gradle");
        String buildGradleContents = Files.readString(path);

        assertThat(buildGradleContents)
                .contains("artifactId = 'jobrunr-kotlin-" + kotlinVersion + "-support'")
                .contains("mavenJava(MavenPublication)");
    }

    private void checkKoinJobActivator(String kotlinSupportModule) {
        Path kotlinXJobMapperTest = Paths.get(kotlinSupportModule, "src/main/kotlin/org/jobrunr/kotlin/di/KoinJobActivator.kt");
        assertThat(Files.exists(kotlinXJobMapperTest)).describedAs("Could not KoinJobActivator in Kotlin module '" + kotlinSupportModule + "'").isTrue();
    }

    private void checkKotlinSerializationPresent(String kotlinSupportModule) {
        int kotlinVersion = parseInt(substringAfterLast(kotlinSupportModule.replace("-support", ""), "-"));
        if (kotlinVersion < 21) return;
        Path kotlinXJobMapperTest = Paths.get(kotlinSupportModule, "src/test/kotlin/org/jobrunr/jobs/mappers/KotlinxSerializationJobMapperTest.kt");
        assertThat(Files.exists(kotlinXJobMapperTest)).describedAs("Could not find KotlinX support in Kotlin module '" + kotlinSupportModule + "'").isTrue();

        Path kotlinXJSonMapperTest = Paths.get(kotlinSupportModule, "src/test/kotlin/org/jobrunr/jobs/mappers/KotlinxSerializationJsonMapperTest.kt");
        assertThat(Files.exists(kotlinXJSonMapperTest)).describedAs("Could not find KotlinX support in Kotlin module '" + kotlinSupportModule + "'").isTrue();
    }
}
