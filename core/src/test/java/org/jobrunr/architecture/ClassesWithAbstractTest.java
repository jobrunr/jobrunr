package org.jobrunr.architecture;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.extension.ForAllSubclassesExtension;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = { "org.jobrunr" })
public class ClassesWithAbstractTest {

    @ArchTest
    ArchRule classesNamedAbstractWithJUnitExtensionShouldBeAbstractAsWell = classes()
            .that().haveSimpleNameContaining("Abstract")
            .and().areAnnotatedWith(ExtendWith.class)
            .should(beAbstractIfForAllSubclassesExtension());

    private static class AbstractForAllSubclassesExtension extends ArchCondition<JavaClass> {

        public AbstractForAllSubclassesExtension(Object... args) {
            super("@ExtendWith(ForAllSubclassesExtension.class) for abstract classes", args);
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
            JavaAnnotation<?> annotation = javaClass.getAnnotationOfType(ExtendWith.class.getTypeName());
            JavaClass[] value = (JavaClass[]) annotation.get("value").get();

            if(value[0].isEquivalentTo(ForAllSubclassesExtension.class) && !javaClass.getModifiers().contains(JavaModifier.ABSTRACT)) {
                conditionEvents.add(SimpleConditionEvent.violated(javaClass, "Java class " + javaClass.getName() + " is not abstract"));
            }
        }
    }

    private ArchCondition<JavaClass> beAbstractIfForAllSubclassesExtension() {
        return new AbstractForAllSubclassesExtension();
    }

}
