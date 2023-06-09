package org.jobrunr.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jobrunr.SevereJobRunrException;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "org.jobrunr", importOptions = {ImportOption.DoNotIncludeTests.class, PackageDependenciesTest.DoNotIncludeTestFixtures.class})
class ExceptionsTest {

    @ArchTest
    ArchRule classesImplementingDiagnosticAwareShouldBeAnException = classes()
            .that().implement(SevereJobRunrException.DiagnosticsAware.class)
            .should().beAssignableTo(Exception.class);
}
