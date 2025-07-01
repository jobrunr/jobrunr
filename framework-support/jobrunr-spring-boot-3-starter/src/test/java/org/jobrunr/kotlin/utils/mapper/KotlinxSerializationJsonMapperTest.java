package org.jobrunr.kotlin.utils.mapper;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class KotlinxSerializationJsonMapperTest {

    // To prevent Class.forName() from breaking when we move or rename the real thing.
    @Test
    void keepPackagesInSyncWithReaLanguageSupportClass() {
        var src = readKotlinSourceFile();

        assertThat(src).contains("package " + KotlinxSerializationJsonMapper.class.getPackageName());
        assertThat(src).contains("class " + KotlinxSerializationJsonMapper.class.getSimpleName());
    }

    // To prevent getDeclaredConstructor().newInstance() from breaking.
    @Test
    void verifyConstructorWithoutArgumentsIsPresentInReaLanguageSupportClass() {
        var src = readKotlinSourceFile();

        assertThat(src).contains("constructor()");
    }

    private String readKotlinSourceFile() {
        try {
            return new String(Files.readAllBytes(getPathForSource("../../language-support/jobrunr-kotlin-22-support/src/main/kotlin/", KotlinxSerializationJsonMapper.class)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getPathForSource(String prefix, Class<?> clazz) {
        return Paths.get(prefix + clazz.getCanonicalName().replaceAll("\\.", "/") + ".kt");
    }

}