package com.spring.test.yaml.junit;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.stream.Stream;

/**
 * Đọc {@link YamlTestSource} và sinh một {@link Arguments} cho mỗi test case enabled trong YAML.
 */
public class YamlTestArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        String yamlPath = AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), YamlTestSource.class)
                .map(YamlTestSource::value)
                .orElseGet(() -> AnnotationSupport.findAnnotation(context.getRequiredTestClass(), YamlTest.class)
                        .map(YamlTest::value)
                        .orElseThrow(() -> new IllegalStateException(
                                "@YamlTestSource or @YamlTest required on " + context.getRequiredTestClass().getName())));

        return YamlTestCases.enabledArguments(yamlPath);
    }
}
