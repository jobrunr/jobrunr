package org.jobrunr.architecture;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "org.jobrunr", importOptions = {ImportOption.DoNotIncludeTests.class, PackageDependenciesTest.DoNotIncludeTestFixtures.class})
public class ClassNamesTest {

    @ArchTest
    ArchRule classesNamedAbstractShouldBeAbstractAsWell = classes()
            .that().haveSimpleNameStartingWith("Abstract")
            .should().haveModifier(JavaModifier.ABSTRACT);

}
