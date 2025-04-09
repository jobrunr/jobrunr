package org.jobrunr.architecture;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extension.ForAllSubclassesExtension;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "org.jobrunr", importOptions = {ImportOption.DoNotIncludeTests.class, PackageDependenciesTest.DoNotIncludeTestFixtures.class})
public class ClassNamesTest {

    @ArchTest
    ArchRule classesNamedAbstractShouldBeAbstractAsWell = classes()
            .that().haveSimpleNameStartingWith("Abstract")
            .should().haveModifier(JavaModifier.ABSTRACT);

}
