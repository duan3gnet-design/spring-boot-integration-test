package com.spring.test.yaml;

import com.spring.test.yaml.model.DbVerifySpec;
import com.spring.test.yaml.model.TestCase;
import com.spring.test.yaml.model.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Điều phối đọc YAML, apply mock (Mockito + WireMock), chạy HTTP test cases,
 * và verify database sau mỗi test case.
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

    // ── Suite ─────────────────────────────────────────────────────────────

    public TestSuite loadSuite(String classpathYaml) {
        return reader.loadClasspath(classpathYaml);
    }

    public void runSuite(String classpathYaml) {
        TestSuite suite = loadSuite(classpathYaml);
        log.info("Running YAML suite: {}", suite.getSuite() != null ? suite.getSuite() : classpathYaml);

        YamlMockConfigurer mockConfigurer = new YamlMockConfigurer(
                context.applicationContext(), context.jsonMapper());
        YamlWireMockConfigurer wireMockConfigurer = new YamlWireMockConfigurer(
                context.wireMockRegistry(), context.jsonMapper());
        YamlDbVerifier dbVerifier = buildDbVerifier();

        mockConfigurer.applyMocks(suite.getMocks());
        wireMockConfigurer.applyStubs(suite.getWireMocks());

        YamlMockMvcTestRunner runner = new YamlMockMvcTestRunner(context.mockMvc(), context.jsonMapper());
        List<String> errors = new ArrayList<>();

        for (TestCase testCase : suite.getTests()) {
            if (!testCase.isEnabled()) {
                continue;
            }
            try {
                mockConfigurer.applyMocks(testCase.getMocks());
                wireMockConfigurer.applyStubs(testCase.getWireMocks());

                runner.run(testCase);

                // DB verify sau khi HTTP request thành công
                verifyDb(dbVerifier, testCase.getDbVerify(), testCase.displayName(), classpathYaml);

                log.debug("PASSED: {}", testCase.displayName());
            } catch (AssertionError | Exception e) {
                errors.add(testCase.displayName() + ": " + e.getMessage());
                log.error("FAILED: {} — {}", testCase.displayName(), e.getMessage());
            } finally {
                mockConfigurer.resetMocks(testCase.getMocks());
                wireMockConfigurer.resetStubs(testCase.getWireMocks());
            }
        }

        // Suite-level DB verify (chạy sau tất cả test cases)
        if (!suite.getDbVerify().isEmpty()) {
            try {
                verifyDb(dbVerifier, suite.getDbVerify(), suite.getSuite() + " [suite]", classpathYaml);
            } catch (AssertionError | Exception e) {
                errors.add("[suite-level dbVerify]: " + e.getMessage());
            }
        }

        mockConfigurer.resetMocks(suite.getMocks());
        wireMockConfigurer.resetStubs(suite.getWireMocks());

        if (!errors.isEmpty()) {
            throw new AssertionError("YAML test failures:\n- " + String.join("\n- ", errors));
        }
    }

    // ── Single case ───────────────────────────────────────────────────────

    public void runCase(String classpathYaml, String testKey) {
        TestSuite suite = loadSuite(classpathYaml);
        TestCase testCase = suite.getTests().stream()
                .filter(t -> testKey.equals(t.getName()) || testKey.equals(t.displayName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + testKey));

        YamlMockConfigurer mockConfigurer = new YamlMockConfigurer(
                context.applicationContext(), context.jsonMapper());
        YamlWireMockConfigurer wireMockConfigurer = new YamlWireMockConfigurer(
                context.wireMockRegistry(), context.jsonMapper());
        YamlDbVerifier dbVerifier = buildDbVerifier();

        mockConfigurer.applyMocks(suite.getMocks());
        wireMockConfigurer.applyStubs(suite.getWireMocks());
        mockConfigurer.applyMocks(testCase.getMocks());
        wireMockConfigurer.applyStubs(testCase.getWireMocks());

        try {
            new YamlMockMvcTestRunner(context.mockMvc(), context.jsonMapper()).run(testCase);
            verifyDb(dbVerifier, testCase.getDbVerify(), testCase.displayName(), classpathYaml);
        } catch (Exception e) {
            throw new AssertionError("YAML test failed: " + testKey + ", " + e.getMessage(), e);
        } finally {
            wireMockConfigurer.resetStubs(testCase.getWireMocks());
            mockConfigurer.resetMocks(testCase.getMocks());
            wireMockConfigurer.resetStubs(suite.getWireMocks());
            mockConfigurer.resetMocks(suite.getMocks());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private YamlDbVerifier buildDbVerifier() {
        if (context.dataSource() != null) {
            return new YamlDbVerifier(context.dataSource(), context.jsonMapper());
        }
        return null;
    }

    private void verifyDb(YamlDbVerifier dbVerifier, List<DbVerifySpec> specs,
                          String testName, String classpathYaml) throws Exception {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        if (dbVerifier == null) {
            throw new IllegalStateException(
                    "Test '" + testName + "' declares dbVerify but YamlTestContext.dataSource() returns null. "
                    + "Override dataSource() in your test class.");
        }
        dbVerifier.verify(specs, testName, classpathYaml);
    }
}
