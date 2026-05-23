package com.spring.test.yaml;

import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * Context cần thiết để chạy YAML-driven tests.
 *
 * <p>Implement {@link #wireMockRegistry()} nếu test cần stub HTTP ra ngoài qua WireMock.
 */
public interface YamlTestContext {

    MockMvc mockMvc();

    JsonMapper jsonMapper();

    ApplicationContext applicationContext();

    /**
     * Registry các WireMock server — override nếu test dùng WireMock.
     * Mặc định trả về registry rỗng (không apply WireMock stub).
     */
    default WireMockServerRegistry wireMockRegistry() {
        return new WireMockServerRegistry();
    }
}
