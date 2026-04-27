package org.jobrunr.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jobrunr.utils.reflection.annotations.Constructor;
import org.jobrunr.utils.reflection.annotations.Field;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "org.jobrunr", importOptions = {ImportOption.DoNotIncludeTests.class, PackageDependenciesTest.DoNotIncludeTestFixtures.class})
public class ReflectionTest {

    @ArchTest
    ArchRule allFieldsMustBeMappedInAnnotatedConstructorIfHavingConstructorAnnotation = classes()
            .should(haveProperlyAnnotatedConstructors());

    private static ArchCondition<JavaClass> haveProperlyAnnotatedConstructors() {
        return new ArchCondition<>("have @Field annotations for every parameter in @Constructor") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                List<JavaConstructor> annotatedConstructors = item.getConstructors().stream()
                        .filter(c -> c.isAnnotatedWith(Constructor.class))
                        .collect(Collectors.toList());

                if (annotatedConstructors.isEmpty()) return;

                if (annotatedConstructors.size() > 1) {
                    events.add(SimpleConditionEvent.violated(item, item.getSimpleName() + " has " + annotatedConstructors.size() + " @Constructor where only one is allowed"));
                    return;
                }

                Set<String> allFieldNames = item.getAllFields().stream()
                        .filter(field -> !field.getModifiers().contains(JavaModifier.STATIC))
                        .map(JavaMember::getName)
                        .collect(Collectors.toSet());

                JavaConstructor constructor = annotatedConstructors.get(0);

                for (JavaParameter parameter : constructor.getParameters()) {
                    if (!parameter.isAnnotatedWith(Field.class)) {
                        events.add(SimpleConditionEvent.violated(item, String.format("Class %s: Parameter %d in @Constructor is missing @Field annotation.", item.getSimpleName(), parameter.getIndex())));
                        continue;
                    }

                    String mappingKey = parameter.getAnnotationOfType(Field.class).value();
                    if (!allFieldNames.remove(mappingKey)) {
                        events.add(SimpleConditionEvent.violated(item, String.format(
                                "Class %s: Constructor parameter %d has @Field(\"%s\"), but no field (local or inherited) is annotated with @Field(\"%s\").",
                                item.getSimpleName(), parameter.getIndex(), mappingKey, mappingKey)));
                    }
                }

                allFieldNames.forEach(fieldName -> events.add(SimpleConditionEvent.violated(item, String.format(
                        "Class %s: Field %s has no constructor parameter with @Field(\"%s\").",
                        item.getSimpleName(), fieldName, fieldName))));
            }
        };
    }
}
