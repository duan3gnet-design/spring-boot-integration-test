package com.spring.test.transaction.integration;

import com.spring.test.yaml.junit.YamlTest;
import com.spring.test.yaml.junit.YamlTestSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests — mỗi case trong YAML = một dòng riêng trên test report.
 *
 * @see "src/test/resources/yaml/transaction-tests.yml"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@YamlTest("yaml/transaction-tests.yml")
@DisplayName("Transaction API Integration Tests (YAML)")
class TransactionIntegrationTest extends TransactionYamlTestBase {

    @ParameterizedTest(name = "[{index}] {0} — {1}")
    @YamlTestSource("yaml/transaction-tests.yml")
    void runYamlCase(String testName, String description) {
        runYamlTest(testName);
    }
}
