package org.jobrunr.storage.sql;

import org.jobrunr.storage.StorageProviderUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class SqlScriptsTest {

    public static final Map<String, Class<?>> TABLES_WITH_FIELDS = Map.of(
            StorageProviderUtils.Migrations.NAME, StorageProviderUtils.Migrations.class,
            StorageProviderUtils.Metadata.NAME, StorageProviderUtils.Metadata.class,
            StorageProviderUtils.Jobs.NAME, StorageProviderUtils.Jobs.class,
            StorageProviderUtils.RecurringJobs.NAME, StorageProviderUtils.RecurringJobs.class,
            StorageProviderUtils.BackgroundJobServers.NAME.replace("_", ""), StorageProviderUtils.BackgroundJobServers.class,
            StorageProviderUtils.JobStats.NAME, StorageProviderUtils.JobStats.class);

    @Test
    public void testSqlScriptsHaveCorrectColumnNamesAndCasing() throws Exception {
        List<FileWithContent> allSqlFiles = getAllSqlFiles();

        allSqlFiles.stream()
                .filter(this::isNotAnEmptyMigration)
                .forEach(this::testNamingAndCasing);
    }

    private void testNamingAndCasing(FileWithContent fileWithContent) {
        List<String> relevantTablesWithFields = TABLES_WITH_FIELDS.keySet().stream()
                .filter(tableName -> fileWithContent.content.toLowerCase().contains("jobrunr_" + tableName))
                .collect(toList());

        assertThat(relevantTablesWithFields)
                .describedAs("Did not find a matching table for %s", fileWithContent.path)
                .isNotEmpty();

        relevantTablesWithFields.forEach(tableName -> testNamingAndCasingForAllFieldsDefinedIn(fileWithContent, TABLES_WITH_FIELDS.get(tableName)));
    }

    private void testNamingAndCasingForAllFieldsDefinedIn(FileWithContent fileWithContent, Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            try {
                if (isNotPublicStatic(field)) continue;

                String fieldName = field.getName();
                String fieldValue = field.get(clazz).toString();
                testNamingAndCasingForSqlField(fileWithContent, clazz.getName() + "." + fieldName, fieldValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean isNotPublicStatic(Field field) {
        int modifiers = field.getModifiers();
        return !(Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers));
    }

    private void testNamingAndCasingForSqlField(FileWithContent fileWithContent, String storageProviderReference, String fieldValue) {
        if (!fileWithContent.content.toLowerCase().contains(fieldValue.toLowerCase())) return;

        int countWithoutCasing = countOccurrences(fileWithContent.content.toLowerCase(), fieldValue.toLowerCase());
        int countWithCasing = countOccurrences(fileWithContent.content, fieldValue);
        assertThat(countWithoutCasing)
                .describedAs("Found invalid casing in " + fileWithContent.path + " for field " + storageProviderReference + " with value " + fieldValue)
                .isEqualTo(countWithCasing);
    }

    private int countOccurrences(String text, String substringToCount) {
        return text.split("(?<!')\\b" + substringToCount + "\\b(?!')").length - 1;
    }

    private boolean isNotAnEmptyMigration(FileWithContent fileWithContent) {
        return !fileWithContent.content.startsWith("-- Empty migration");
    }

    public List<FileWithContent> getAllSqlFiles() throws Exception {
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
    }
}
