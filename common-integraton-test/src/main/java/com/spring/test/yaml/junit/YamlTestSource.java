package com.spring.test.yaml.junit;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nguồn {@link org.junit.jupiter.params.ParameterizedTest} từ file YAML.
 * <p>
 * Dùng trên method:
 * <pre>{@code
 * @ParameterizedTest(name = "[{index}] {0}")
 * @YamlTestSource("yaml/transaction-tests.yml")
 * void runCase(String testName) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(YamlTestArgumentsProvider.class)
public @interface YamlTestSource {

    String value();
}
