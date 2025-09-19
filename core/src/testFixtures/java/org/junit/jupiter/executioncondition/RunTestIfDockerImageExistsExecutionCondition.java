package org.junit.jupiter.executioncondition;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

/**
 * Class which determines if the given docker image is present.
 * <p>
 * We cannot use com.github.docker-java:docker-java as that project pulls in Jackson as
 * dependency and we then cannot test anymore whether the project works with only Gson.
 */
public class RunTestIfDockerImageExistsExecutionCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<RunTestIfDockerImageExists> runTestIfDockerImageExistsOptional = findAnnotation(element, RunTestIfDockerImageExists.class);
        if (runTestIfDockerImageExistsOptional.isPresent()) {
            final RunTestIfDockerImageExists runTestIfDockerImageExists = runTestIfDockerImageExistsOptional.get();
            final String imageTag = runTestIfDockerImageExists.value();

            boolean foundDockerImage = false;
            try {
                final Process process = Runtime.getRuntime().exec(String.format("docker images %s", imageTag));
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(imageTag.split(":")[0])) {
                            foundDockerImage = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // ignored
            }

            if (foundDockerImage) {
                return ConditionEvaluationResult.enabled(String.format("Test enabled as docker image %s is available.", imageTag));
            } else {
                String reason = String.format("Could not determine whether docker image %s is available.", imageTag);
                System.err.println("Test disabled because of @RunTestIfDockerImageExists - " + reason);
                return ConditionEvaluationResult.disabled(reason);
            }
        }
        return ConditionEvaluationResult.enabled("@RunTestIfDockerImageExists is not present.");
    }
}
