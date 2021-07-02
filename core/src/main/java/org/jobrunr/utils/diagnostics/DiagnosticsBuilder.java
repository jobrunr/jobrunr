package org.jobrunr.utils.diagnostics;

import org.jobrunr.utils.io.IOUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getClassLocationOfLambda;

public class DiagnosticsBuilder {

    private final List<DiagnosticsItem> diagnosticsItems;

    private DiagnosticsBuilder() {
        this.diagnosticsItems = new ArrayList<>();
    }

    public static DiagnosticsBuilder diagnostics() {
        return new DiagnosticsBuilder();
    }

    public DiagnosticsBuilder withTitle(String title) {
        diagnosticsItems.add(new DiagnosticsTitle(title));
        return this;
    }

    public DiagnosticsBuilder withSubTitle(String title) {
        diagnosticsItems.add(new DiagnosticsTitle(1, title));
        return this;
    }

    public DiagnosticsBuilder withLine(String line) {
        diagnosticsItems.add(new DiagnosticsLine(line));
        return this;
    }

    public DiagnosticsBuilder withIndentedLine(String line) {
        diagnosticsItems.add(new DiagnosticsLine(1, line));
        return this;
    }

    public DiagnosticsBuilder withIndentedLine(int amountOfIndentation, String line) {
        diagnosticsItems.add(new DiagnosticsLine(amountOfIndentation, line));
        return this;
    }

    public DiagnosticsBuilder withBulletedLine(String line) {
        diagnosticsItems.add(new DiagnosticsBulletedLine(line));
        return this;
    }

    public DiagnosticsBuilder withBulletedLine(String key, String value) {
        diagnosticsItems.add(new DiagnosticsBulletedLine(key, value));
        return this;
    }

    public DiagnosticsBuilder withIndentedBulletedLine(String key, String value) {
        diagnosticsItems.add(new DiagnosticsBulletedLine(1, key, value));
        return this;
    }

    public DiagnosticsBuilder withBulletedTextBlock(String key, String textBlock) {
        diagnosticsItems.add(new DiagnosticsBulletedLine(key, "\n" + textBlock));
        return this;
    }

    public DiagnosticsBuilder withEmptyLine() {
        diagnosticsItems.add(new DiagnosticsLine(""));
        return this;
    }

    public <T> DiagnosticsBuilder with(List<T> items, BiConsumer<T, DiagnosticsBuilder> consumer) {
        if (items.isEmpty()) {
            withLine("No items available");
        } else {
            items.forEach(item -> consumer.accept(item, this));
        }
        return this;
    }

    public DiagnosticsBuilder withObject(Object object) {
        return this.withObject("object", object);
    }

    public DiagnosticsBuilder withObject(String key, Object object) {
        return this.withBulletedLine(key, object.toString() + "(" + object.getClass().getName() + ")");
    }

    public DiagnosticsBuilder withParameterTypes(Class<?>[] paramTypes) {
        return this.withBulletedLine("parameterTypes", stream(paramTypes).map(Class::getName).collect(joining(", ")));
    }

    public DiagnosticsBuilder withParameters(Object[] parameters) {
        return this.withBulletedLine("parameters", stream(parameters).map(o -> o.toString() + "(" + o.getClass().getName() + ")").collect(joining(", ")));
    }

    public DiagnosticsBuilder withException(Exception exception) {
        diagnosticsItems.add(new DiagnosticsException(exception));
        return this;
    }

    public DiagnosticsBuilder withDiagnostics(int shiftTitle, DiagnosticsBuilder diagnostics) {
        List<DiagnosticsItem> diagnosticsItems = diagnostics.diagnosticsItems.stream()
                .map(item -> shiftDiagnosticsTitle(shiftTitle, item))
                .collect(toList());

        this.diagnosticsItems.addAll(diagnosticsItems);
        return this;
    }

    public String asMarkDown() {
        StringBuilder result = new StringBuilder();
        diagnosticsItems.forEach(diagnosticsItem -> result.append(diagnosticsItem.toMarkdown()));
        return result.toString();
    }

    public DiagnosticsBuilder withLambda(Object lambda) {
        String location = getClassLocationOfLambda(lambda);
        URL resource = lambda.getClass().getResource(location);

        return this
                .withBulletedLine("lambda", lambda.toString())
                .withBulletedLine("lambda location", location)
                .withBulletedTextBlock("class file", disassembleClassFromJava(resource.toExternalForm()));
    }

    String disassembleClassFromJava(String resourceFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder("javap", "-c", resourceFile)
                    .redirectErrorStream(true);

            final Process process = pb.start();
            final StringWriter writer = new StringWriter();
            new Thread(() -> IOUtils.copyStreamNoException(process.getInputStream(), writer)).start();

            final int exitValue = process.waitFor();
            final String processOutput = writer.toString();
            return processOutput;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Unable to run javap command (" + e.getMessage() + ").";
        } catch (IOException e) {
            return "Unable to run javap command (" + e.getMessage() + ").";
        }
    }

    private DiagnosticsItem shiftDiagnosticsTitle(int amount, DiagnosticsItem diagnosticsItem) {
        if (diagnosticsItem instanceof DiagnosticsTitle) {
            return new DiagnosticsTitle(amount, (DiagnosticsTitle) diagnosticsItem);
        } else {
            return diagnosticsItem;
        }
    }
}

