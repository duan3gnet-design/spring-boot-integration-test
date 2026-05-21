package com.spring.test.yaml.junit;

import com.spring.test.yaml.YamlTestDataReader;
import com.spring.test.yaml.model.TestCase;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * Cung cấp test cases từ YAML cho {@code @ParameterizedTest}.
 */
public final class YamlTestCases {

    private YamlTestCases() {}

    public static Stream<String> enabledNames(String classpathYaml) {
        return enabledCases(classpathYaml).map(TestCase::getName);
    }

    public static Stream<TestCase> enabledCases(String classpathYaml) {
        return new YamlTestDataReader().loadClasspath(classpathYaml).getTests().stream()
                .filter(TestCase::isEnabled)
                .filter(tc -> tc.getName() != null && !tc.getName().isBlank());
    }

    /** Mỗi dòng = một test riêng trong report: tên + mô tả. */
    public static Stream<Arguments> enabledArguments(String classpathYaml) {
        return enabledCases(classpathYaml)
                .map(tc -> Arguments.of(
                        tc.getName(),
                        tc.getDescription() != null && !tc.getDescription().isBlank()
                                ? tc.getDescription()
                                : tc.displayName()));
    }
}
