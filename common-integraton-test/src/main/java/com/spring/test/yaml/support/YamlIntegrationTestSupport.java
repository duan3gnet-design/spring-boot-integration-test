package com.spring.test.yaml.support;

import com.spring.test.yaml.YamlTestContext;
import com.spring.test.yaml.YamlTestExecutor;
import com.spring.test.yaml.junit.YamlTest;
import com.spring.test.yaml.junit.YamlTestCases;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * Mixin cho class test — cung cấp helper chạy YAML từ {@link YamlTestContext}.
 */
public interface YamlIntegrationTestSupport extends YamlTestContext {

    default void runYamlTests(String classpathYaml) {
        new YamlTestExecutor(this).runSuite(classpathYaml);
    }

    default void runYamlTest(String classpathYaml, String testName) {
        new YamlTestExecutor(this).runCase(classpathYaml, testName);
    }

    default void runYamlTest(String testName) {
        runYamlTest(yamlClasspath(), testName);
    }

    default String yamlClasspath() {
        YamlTest annotation = getClass().getAnnotation(YamlTest.class);
        if (annotation == null || annotation.value().isBlank()) {
            throw new IllegalStateException("@YamlTest is required on " + getClass().getName());
        }
        return annotation.value();
    }

    default Stream<String> enabledYamlTestNames() {
        return YamlTestCases.enabledNames(yamlClasspath());
    }

    default Stream<Arguments> enabledYamlTestArguments() {
        return YamlTestCases.enabledArguments(yamlClasspath());
    }
}
