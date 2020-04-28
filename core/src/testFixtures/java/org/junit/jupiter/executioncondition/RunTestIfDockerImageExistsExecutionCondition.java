package org.junit.jupiter.executioncondition;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class RunTestIfDockerImageExistsExecutionCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AnnotatedElement> element = context.getElement();
        Optional<RunTestIfDockerImageExists> runTestIfDockerImageExistsOptional = findAnnotation(element, RunTestIfDockerImageExists.class);
        if (runTestIfDockerImageExistsOptional.isPresent()) {
            final RunTestIfDockerImageExists runTestIfDockerImageExists = runTestIfDockerImageExistsOptional.get();
            final DockerClient dockerClient = DockerClientBuilder.getInstance().build();
            final String imageId = runTestIfDockerImageExists.value();
            final List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(imageId).exec();
            if (images.isEmpty()) {
                return ConditionEvaluationResult.disabled(String.format("Test disabled as docker image %s is not found in local docker image registry", imageId));
            }
            return ConditionEvaluationResult.enabled(String.format("Test enabled as docker image %s is available.", imageId));
        }
        return ConditionEvaluationResult.enabled("@RunTestIfDockerImageExists is not present.");
    }
}
