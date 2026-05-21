package com.spring.test.yaml;

import com.spring.test.yaml.model.MockSpec;
import com.spring.test.yaml.model.TestCase;
import com.spring.test.yaml.model.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Điều phối đọc YAML, apply mock, chạy HTTP test cases.
 */
public class YamlTestExecutor {

    private static final Logger log = LoggerFactory.getLogger(YamlTestExecutor.class);

    private final YamlTestDataReader reader;
    private final YamlTestContext context;

    public YamlTestExecutor(YamlTestContext context) {
        this(context, new YamlTestDataReader());
    }

    public YamlTestExecutor(YamlTestContext context, YamlTestDataReader reader) {
        this.context = context;
        this.reader = reader;
    }

    public TestSuite loadSuite(String classpathYaml) {
        return reader.loadClasspath(classpathYaml);
    }

    public void runSuite(String classpathYaml) {
        TestSuite suite = loadSuite(classpathYaml);
        log.info("Running YAML suite: {}", suite.getSuite() != null ? suite.getSuite() : classpathYaml);

        YamlMockConfigurer mockConfigurer = new YamlMockConfigurer(
                context.applicationContext(), context.jsonMapper());
        mockConfigurer.applyMocks(suite.getMocks());

        YamlMockMvcTestRunner runner = new YamlMockMvcTestRunner(context.mockMvc(), context.jsonMapper());
        List<String> errors = new ArrayList<>();

        for (TestCase testCase : suite.getTests()) {
            if (!testCase.isEnabled()) {
                continue;
            }
            try {
                mockConfigurer.applyMocks(testCase.getMocks());
                runner.run(testCase);
                log.debug("PASSED: {}", testCase.displayName());
            } catch (AssertionError | Exception e) {
                errors.add(testCase.displayName() + ": " + e.getMessage());
                log.error("FAILED: {} — {}", testCase.displayName(), e.getMessage());
            } finally {
                mockConfigurer.resetMocks(testCase.getMocks());
            }
        }

        mockConfigurer.resetMocks(suite.getMocks());

        if (!errors.isEmpty()) {
            throw new AssertionError("YAML test failures:\n- " + String.join("\n- ", errors));
        }
    }

    public void runCase(String classpathYaml, String testKey) {
        TestSuite suite = loadSuite(classpathYaml);
        TestCase testCase = suite.getTests().stream()
                .filter(t -> testKey.equals(t.getName()) || testKey.equals(t.displayName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + testKey));

        YamlMockConfigurer mockConfigurer = new YamlMockConfigurer(
                context.applicationContext(), context.jsonMapper());
        mockConfigurer.applyMocks(suite.getMocks());
        mockConfigurer.applyMocks(testCase.getMocks());

        try {
            new YamlMockMvcTestRunner(context.mockMvc(), context.jsonMapper()).run(testCase);
        } catch (Exception e) {
            throw new AssertionError("YAML test failed: " + testKey, e);
        } finally {
            mockConfigurer.resetMocks(testCase.getMocks());
            mockConfigurer.resetMocks(suite.getMocks());
        }
    }
}
