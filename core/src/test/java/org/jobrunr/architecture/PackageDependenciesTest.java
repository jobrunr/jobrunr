package org.jobrunr.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jobrunr.JobRunrException;
import org.jobrunr.architecture.PackageDependenciesTest.DoNotIncludeTestFixtures;
import org.jobrunr.server.BackgroundJobPerformer;
import org.jobrunr.server.Java11OrHigherInternalDesktopUtil;
import org.jobrunr.server.dashboard.DashboardNotification;
import org.jobrunr.utils.reflection.autobox.InstantForOracleTypeAutoboxer;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "org.jobrunr", importOptions = {DoNotIncludeTests.class, DoNotIncludeTestFixtures.class})
class PackageDependenciesTest {

    static class DoNotIncludeTestFixtures implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.toString().contains("test-fixtures");
        }
    }

    @ArchTest
    ArchRule jobRunrDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.configuration..").and().haveSimpleName("JobRunr")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "java..");

    @ArchTest
    ArchRule jobRunrConfigurationDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.configuration..").and().haveSimpleName("JobRunrConfiguration")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "java..");

    @ArchTest
    ArchRule jobRunrDashboardClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.dashboard..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "com.sun..", "org.slf4j..", "java..");

    @ArchTest
    ArchRule jobDashboardClassesShouldNotDependOnServerClasses = noClasses()
            .that().resideInAPackage("org.jobrunr.dashboard..")
            .should().onlyDependOnClassesThat(
                    resideInAnyPackage("org.jobrunr.server..")
                            .or(are(assignableFrom(DashboardNotification.class)))
            );

    @ArchTest
    ArchRule jobRunrJobsClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.jobs..")
            .and().resideOutsideOfPackage("org.jobrunr.jobs.details..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "org.slf4j..", "java..")
            .orShould().beAnonymousClasses(); // needed to make switch case work.

    @ArchTest
    ArchRule jobRunrJobsDetailsClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.jobs.details..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "org.objectweb.asm..", "org.slf4j..", "java..");

    @ArchTest
    ArchRule jobRunrSchedulingClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.scheduling..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "org.slf4j..", "java..");

    @ArchTest
    ArchRule jobSchedulingClassesShouldNotDependOnServerClasses = noClasses()
            .that().resideInAPackage("org.jobrunr.scheduling..")
            .should().dependOnClassesThat().resideInAnyPackage("org.jobrunr.server..");

    @ArchTest
    ArchRule jobRunrServerClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.server..")
            .and().resideOutsideOfPackage("org.jobrunr.server.jmx..")
            .and().resideOutsideOfPackage("org.jobrunr.server.metrics..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "org.slf4j..", "java..");

    @ArchTest
    ArchRule jobRunrServerClassesShouldNotDependOnJavaAwtDependenciesTest = noClasses()
            .that().resideInAPackage("org.jobrunr.server..")
            .and().doNotHaveFullyQualifiedName(Java11OrHigherInternalDesktopUtil.class.getName())
            .should().dependOnClassesThat().resideInAPackage("java.awt..");

    @ArchTest
    ArchRule jobRunrServerJmxClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.server.jmx..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "java..", "javax.management..");

    @ArchTest
    ArchRule jobServerClassesShouldNotDependOnSchedulingClasses = noClasses()
            .that().resideInAPackage("org.jobrunr.server..").and().areNotAssignableFrom(BackgroundJobPerformer.class)
            .should().dependOnClassesThat().resideInAnyPackage("org.jobrunr.scheduling..");

    @ArchTest
    ArchRule jobServerClassesShouldNotDependOnDashboardClasses = noClasses()
            .that().resideInAPackage("org.jobrunr.server..")
            .should().dependOnClassesThat().resideInAnyPackage("org.jobrunr.dashboard..");

    @ArchTest
    ArchRule jobRunrStorageClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.storage")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "org.jobrunr.server.jmx..", "org.slf4j..", "java..");

    //@ArchTest
    ArchRule jobRunrStorageElasticSearchClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.storage.nosql.elasticsearch..")
            .should().onlyDependOnClassesThat(
                    resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "co.elastic..", "org.elasticsearch.client..", "org.apache.http..", "jakarta.json..", "org.slf4j..", "java..")
                            .or(are(equivalentTo(JobRunrException.class)))
            );

    @ArchTest
    ArchRule jobRunrStorageMongoClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.storage.nosql.mongo..")
            .should().onlyDependOnClassesThat(
                    resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "com.mongodb..", "org.bson..", "org.slf4j..", "java..", "")
                            .or(are(equivalentTo(JobRunrException.class)))
            ); // see https://github.com/TNG/ArchUnit/issues/519

    //@ArchTest
    ArchRule jobRunrStorageRedisJedisClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.storage.nosql.redis..")
            .and().haveSimpleNameStartingWith("Jedis")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "redis.clients..", "org.slf4j..", "java..");

    //@ArchTest
    ArchRule jobRunrStorageRedisLettuceClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.storage.nosql.redis..")
            .and().haveSimpleNameStartingWith("Lettuce")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "io.lettuce..", "org.apache.commons.pool2..", "org.slf4j..", "java..");

    @ArchTest
    ArchRule jobRunrStorageSqlClassesDependenciesTest = classes()
            .that().resideInAnyPackage("org.jobrunr.storage.sql..")
            .and().resideOutsideOfPackage("org.jobrunr.storage.sql.common..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr.jobs..", "org.jobrunr.storage..", "org.jobrunr.utils..", "javax.sql..", "org.slf4j..", "java..");

    @ArchTest
    ArchRule jobRunrUtilsClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.utils..")
            .and().resideOutsideOfPackage("org.jobrunr.utils.mapper..")
            .and().doNotHaveFullyQualifiedName(InstantForOracleTypeAutoboxer.class.getName())
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "org.slf4j..", "java..");

    @ArchTest
    ArchRule jobRunrUtilsGsonMapperClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.utils.mapper.gson..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "com.google.gson..", "java..", "");

    @ArchTest
    ArchRule jobRunrUtilsJacksonMapperClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.utils.mapper.jackson..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "com.fasterxml..", "java..");

    @ArchTest
    ArchRule jobRunrUtilsJsonBMapperClassesDependenciesTest = classes()
            .that().resideInAPackage("org.jobrunr.utils.mapper.jsonb..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("org.jobrunr..", "jakarta.json..", "java..");

    static final class DoNotIncludeMainResources implements ImportOption {

        @Override
        public boolean includes(Location location) {
            if (location.contains("Java11OrHigherInternalDesktopUtil")) {
                System.out.println(location);
                return false;
            }
            if (location.contains("/build/resources/")) {
                return false;
            }
            return true;
        }
    }
}
