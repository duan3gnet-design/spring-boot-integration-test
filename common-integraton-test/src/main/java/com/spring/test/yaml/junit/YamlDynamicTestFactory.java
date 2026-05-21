package com.spring.test.yaml.junit;

import com.spring.test.yaml.YamlTestContext;
import com.spring.test.yaml.YamlTestDataReader;
import com.spring.test.yaml.YamlTestExecutor;
import com.spring.test.yaml.model.TestCase;
import com.spring.test.yaml.model.TestSuite;
import org.junit.jupiter.api.DynamicTest;

import java.util.stream.Stream;

/**
 * Tạo {@link DynamicTest} từ file YAML — dùng trong {@code @TestFactory}.
 */
public final class YamlDynamicTestFactory {

    private YamlDynamicTestFactory() {}

    public static Stream<DynamicTest> stream(YamlTestContext context, String classpathYaml) {
        TestSuite suite = new YamlTestDataReader().loadClasspath(classpathYaml);
        YamlTestExecutor executor = new YamlTestExecutor(context);
        return suite.getTests().stream()
                .filter(TestCase::isEnabled)
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.displayName(),
                        () -> executor.runCase(classpathYaml, resolveKey(testCase))));
    }

    private static String resolveKey(TestCase testCase) {
        if (testCase.getName() != null && !testCase.getName().isBlank()) {
            return testCase.getName();
        }
        return testCase.displayName();
    }
}
