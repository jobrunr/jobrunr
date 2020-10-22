package org.jobrunr.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.jobrunr.JobRunrException;
import org.jobrunr.utils.reflection.autobox.InstantForOracleTypeAutoboxer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PackageDependenciesTest {

    private static final ImportOption DO_NOT_INCLUDE_TEST_FIXTURES = location -> !location.toString().contains("test-fixtures");

    private JavaClasses classes;

    @BeforeEach
    void setUpJavaClasses() {
        classes = new ClassFileImporter()
                .withImportOption(DO_NOT_INCLUDE_TESTS)
                .withImportOption(DO_NOT_INCLUDE_TEST_FIXTURES)
                .importPackages("org.jobrunr");
    }

    @Test
    void jobRunrConfigurationDependenciesTest() {
        ArchRule configurationClasses = classes()
                .that().resideInAPackage("org.jobrunr.configuration..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "java..");

        configurationClasses.check(classes);
    }

    @Test
    void jobRunrDashboardClassesDependenciesTest() {
        ArchRule dashboardClasses = classes()
                .that().resideInAPackage("org.jobrunr.dashboard..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "com.sun..", "org.slf4j..", "java..");
        dashboardClasses.check(classes);
    }

    @Test
    void jobRunrJobsClassesDependenciesTest() {
        ArchRule jobClassesExceptJobDetails = classes()
                .that().resideInAPackage("org.jobrunr.jobs..").and().resideOutsideOfPackage("org.jobrunr.jobs.details..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "org.slf4j..", "java..");
        jobClassesExceptJobDetails.check(classes);

        ArchRule jobDetailsClasses = classes()
                .that().resideInAPackage("org.jobrunr.jobs.details..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "org.objectweb.asm..", "org.slf4j..", "java..");
        jobDetailsClasses.check(classes);
    }

    @Test
    void jobRunrSchedulingClassesDependenciesTest() {
        ArchRule jobSchedulingClasses = classes()
                .that().resideInAPackage("org.jobrunr.scheduling..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "org.slf4j..", "java..");
        jobSchedulingClasses.check(classes);

        ArchRule jobSchedulingClassesShouldNotDependOnServerClasses = noClasses()
                .that().resideInAPackage("org.jobrunr.scheduling..")
                .should().dependOnClassesThat().resideInAnyPackage("org.jobrunr.server..");
        jobSchedulingClassesShouldNotDependOnServerClasses.check(classes);
    }

    @Test
    void jobRunrServerClassesDependenciesTest() {
        ArchRule jobServerClassesWithoutJmx = classes()
                .that().resideInAPackage("org.jobrunr.server..").and().resideOutsideOfPackage("org.jobrunr.server.jmx..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "org.slf4j..", "java..");
        jobServerClassesWithoutJmx.check(classes);

        ArchRule jobServerJmxClasses = classes()
                .that().resideInAPackage("org.jobrunr.server.jmx..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "java..", "javax.management..");
        jobServerJmxClasses.check(classes);

        ArchRule jobServerClassesShouldNotDependOnSchedulingClasses = noClasses()
                .that().resideInAPackage("org.jobrunr.server..")
                .should().dependOnClassesThat().resideInAnyPackage("org.jobrunr.scheduling..");
        jobServerClassesShouldNotDependOnSchedulingClasses.check(classes);
    }

    @Test
    void jobRunrStorageClassesDependenciesTest() {
        ArchRule jobStorageClasses = classes()
                .that().resideInAPackage("org.jobrunr.storage")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "org.jobrunr.server.jmx..", "org.slf4j..", "java..");
        jobStorageClasses.check(classes);

        ArchRule elasticSearchClasses = classes()
                .that().resideInAPackage("org.jobrunr.storage.nosql.elasticsearch")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "org.elasticsearch..", "org.apache.http..", "org.slf4j..", "java..");
        elasticSearchClasses.check(classes);

        ArchRule mongoClasses = classes()
                .that().resideInAPackage("org.jobrunr.storage.nosql.mongo")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "com.mongodb..", "org.bson..", "org.slf4j..", "java..")
                .orShould().onlyDependOnClassesThat().areAssignableFrom(JobRunrException.class)
                .orShould().onlyDependOnClassesThat().areAssignableFrom(byte.class);
        //mongoClasses.check(classes);

        ArchRule jedisClasses = classes()
                .that().resideInAPackage("org.jobrunr.storage.nosql.redis")
                .and().haveSimpleNameStartingWith("Jedis")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "redis.clients..", "org.slf4j..", "java..");
        jedisClasses.check(classes);

        ArchRule lettuceClasses = classes()
                .that().resideInAPackage("org.jobrunr.storage.nosql.redis")
                .and().haveSimpleNameStartingWith("Lettuce")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "io.lettuce..", "org.apache.commons.pool2..", "org.slf4j..", "java..");
        lettuceClasses.check(classes);

        ArchRule sqlClasses = classes()
                .that().resideInAnyPackage("org.jobrunr.storage.sql..")
                .and().resideOutsideOfPackage("org.jobrunr.storage.sql.common..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "javax.sql..", "org.slf4j..", "java..");
        sqlClasses.check(classes);
    }

    @Test
    void jobRunrUtilsClassesDependenciesTest() {
        ArchRule jobUtilsClasses = classes()
                .that().resideInAPackage("org.jobrunr.utils..")
                .and().resideOutsideOfPackage("org.jobrunr.utils.mapper..")
                .and().doNotHaveFullyQualifiedName(InstantForOracleTypeAutoboxer.class.getName())
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "org.slf4j..", "java..");
        jobUtilsClasses.check(classes);

        ArchRule jobUtilGsonMapperClasses = classes()
                .that().resideInAPackage("org.jobrunr.utils.mapper.gson..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "com.google.gson..", "java..");
        //orShould().onlyDependOnClassesThat().areAssignableFrom(new int[0].getClass());
        //jobUtilGsonMapperClasses.check(classes);

        ArchRule jobUtilJacksonMapperClasses = classes()
                .that().resideInAPackage("org.jobrunr.utils.mapper.jackson..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "com.fasterxml..", "java..");
        jobUtilJacksonMapperClasses.check(classes);

        ArchRule jobUtilJsonBMapperClasses = classes()
                .that().resideInAPackage("org.jobrunr.utils.mapper.jsonb..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "javax.json..", "java..");
        jobUtilJsonBMapperClasses.check(classes);
    }

}
