package org.jobrunr.storage.sql;

import org.jobrunr.storage.StorageProviderUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class SqlScriptsTest {

    @Test
    public void testSqlScriptsHaveCorrectColumnNamesAndCasing() throws Exception {
        List<FileWithContent> allSqlFiles = getAllSqlFiles();

        allSqlFiles.forEach(this::testNamingAndCasing);
    }

    private void testNamingAndCasing(FileWithContent fileWithContent) {
        testNamingAndCasingForAllFieldsDefinedIn(fileWithContent, StorageProviderUtils.Jobs.class);
        testNamingAndCasingForAllFieldsDefinedIn(fileWithContent, StorageProviderUtils.RecurringJobs.class);
        testNamingAndCasingForAllFieldsDefinedIn(fileWithContent, StorageProviderUtils.BackgroundJobServers.class);
    }

    private void testNamingAndCasingForAllFieldsDefinedIn(FileWithContent fileWithContent, Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();   // Get public fields (only public ones)

        for (Field field : fields) {
            try {
                int modifiers = field.getModifiers();
                if (!(Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers))) continue;

                String fieldName = field.getName();
                String fieldValue = field.get(clazz).toString();
                testNamingAndCasingForSqlField(fileWithContent, clazz.getName() + "." + fieldName, fieldValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void testNamingAndCasingForSqlField(FileWithContent fileWithContent, String storageProviderReference, String fieldValue) {
        if (!fileWithContent.getContent().toLowerCase().contains(fieldValue.toLowerCase())) return;

        int countWithoutCasing = fileWithContent.getContent().toLowerCase().split("\\b" + fieldValue.toLowerCase() + "\\b").length - 1;
        int countWithCasing = fileWithContent.getContent().split("\\b" + fieldValue + "\\b").length - 1;
        assertThat(countWithoutCasing)
                .describedAs("Found invalid casing in " + fileWithContent.getPath() + " for field " + storageProviderReference + " with value " + fieldValue)
                .isEqualTo(countWithCasing);
    }


    public List<FileWithContent> getAllSqlFiles() throws Exception {
        // Walk through all files in the directory
        try (Stream<Path> paths = Files.walk(Path.of("src/main/resources/org/jobrunr/storage/sql"))) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .map(FileWithContent::new)
                    .collect(toList());
        }
    }

    public static class FileWithContent {
        private final Path path;
        private final String content;

        public FileWithContent(Path path) {
            try {
                this.path = path;
                this.content = Files.readString(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public Path getPath() {
            return path;
        }

        public String getContent() {
            return content;
        }
    }
}
