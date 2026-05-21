package com.spring.test.yaml;

import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * Context cần thiết để chạy YAML-driven tests.
 */
public interface YamlTestContext {

    MockMvc mockMvc();

    JsonMapper jsonMapper();

    ApplicationContext applicationContext();
}
