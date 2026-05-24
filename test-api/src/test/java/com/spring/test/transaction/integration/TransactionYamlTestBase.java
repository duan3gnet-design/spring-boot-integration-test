package com.spring.test.transaction.integration;

import com.spring.test.transaction.AbstractIntegrationTest;
import com.spring.test.transaction.repository.TransactionRepository;
import com.spring.test.yaml.YamlTestContext;
import com.spring.test.yaml.support.YamlIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;

/**
 * Base class cho YAML-driven integration tests của test-api.
 */
public abstract class TransactionYamlTestBase extends AbstractIntegrationTest
        implements YamlTestContext, YamlIntegrationTestSupport {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected JsonMapper jsonMapper;
    @Autowired protected ApplicationContext applicationContext;
    @Autowired protected TransactionRepository transactionRepository;
    @Autowired protected DataSource dataSource;

    @BeforeEach
    protected void cleanDatabase() {
        transactionRepository.deleteAll();
    }

    @Override
    public MockMvc mockMvc() {
        return mockMvc;
    }

    @Override
    public JsonMapper jsonMapper() {
        return jsonMapper;
    }

    @Override
    public ApplicationContext applicationContext() {
        return applicationContext;
    }

    @Override
    public DataSource dataSource() {
        return dataSource;
    }
}
