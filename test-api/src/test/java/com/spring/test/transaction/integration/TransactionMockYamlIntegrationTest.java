package com.spring.test.transaction.integration;

import com.spring.test.transaction.repository.TransactionRepository;
import com.spring.test.yaml.junit.YamlTest;
import com.spring.test.yaml.junit.YamlTestSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Mock bean từ YAML — mỗi case một dòng trên report.
 *
 * @see src/test/resources/yaml/transaction-mock-tests.yml
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@YamlTest("yaml/transaction-mock-tests.yml")
@DisplayName("Transaction API Mock Tests (YAML)")
class TransactionMockYamlIntegrationTest extends TransactionYamlTestBase {

    @MockitoBean
    TransactionRepository transactionRepository;

    @Override
    @BeforeEach
    protected void cleanDatabase() {
        // Repository là mock — không xóa DB thật
    }

    @ParameterizedTest(name = "[{index}] {0} — {1}")
    @YamlTestSource("yaml/transaction-mock-tests.yml")
    void runYamlCase(String testName, String description) {
        runYamlTest(testName);
    }
}
