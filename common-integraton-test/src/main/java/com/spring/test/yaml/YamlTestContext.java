package com.spring.test.yaml;

import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;

/**
 * Context cần thiết để chạy YAML-driven tests.
 *
 * <p>Override các default method nếu test cần:
 * <ul>
 *   <li>{@link #wireMockRegistry()} — stub HTTP external qua WireMock</li>
 *   <li>{@link #dataSource()} — verify dữ liệu database sau test</li>
 * </ul>
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

    /**
     * DataSource để verify DB — override nếu test dùng {@code dbVerify} trong YAML.
     * Mặc định throw exception khi có dbVerify nhưng chưa cung cấp DataSource.
     */
    default DataSource dataSource() {
        return null;
    }
}
