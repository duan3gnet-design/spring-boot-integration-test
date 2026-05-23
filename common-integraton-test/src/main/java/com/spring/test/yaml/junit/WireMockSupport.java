package com.spring.test.yaml.junit;

import com.spring.test.yaml.WireMockServerRegistry;
import com.spring.test.yaml.YamlTestContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Mixin cho test class dùng {@link EnableWireMock}.
 *
 * <p>Implement interface này để:
 * <ul>
 *   <li>Tự động override {@link YamlTestContext#wireMockRegistry()} — inject đúng registry
 *       vào {@code YamlTestExecutor} mà không cần code thủ công</li>
 *   <li>Truy cập {@link #getWireMockRegistry()} để đọc port trong {@code @DynamicPropertySource}</li>
 * </ul>
 *
 * <p>Yêu cầu: class phải được annotate với {@link EnableWireMock}.
 *
 * <p>Ví dụ đầy đủ:
 * <pre>
 * &#64;SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
 * &#64;AutoConfigureMockMvc
 * &#64;ActiveProfiles("test")
 * &#64;YamlTest("yaml/payment-tests.yml")
 * &#64;EnableWireMock({
 *     &#64;WireMockServer(name = "payment-service"),
 *     &#64;WireMockServer(name = "notification-service")
 * })
 * class PaymentYamlTest extends AbstractIntegrationTest
 *         implements YamlTestContext, YamlIntegrationTestSupport, WireMockSupport {
 *
 *     &#64;Autowired MockMvc mockMvc;
 *     &#64;Autowired JsonMapper jsonMapper;
 *     &#64;Autowired ApplicationContext applicationContext;
 *
 *     &#64;DynamicPropertySource
 *     static void wireMockProperties(DynamicPropertyRegistry registry) {
 *         registry.add("payment.base-url",
 *             () -> "http://localhost:" + getWireMockRegistry().get("payment-service").port());
 *     }
 *
 *     &#64;ParameterizedTest(name = "[{index}] {0}")
 *     &#64;YamlTestSource("yaml/payment-tests.yml")
 *     void runYamlCase(String testName, String description) {
 *         runYamlTest(testName);
 *     }
 * }
 * </pre>
 */
public interface WireMockSupport extends YamlTestContext {

    /**
     * Lấy {@link WireMockServerRegistry} từ JUnit Extension Store.
     *
     * <p>Dùng trong {@code @DynamicPropertySource} để đọc port sau khi server đã start:
     * <pre>
     * registry.add("service.url",
     *     () -> "http://localhost:" + getWireMockRegistry().get("my-service").port());
     * </pre>
     *
     * <p><b>Lưu ý:</b> method này là {@code static} về mặt ngữ nghĩa (dùng trong
     * {@code @DynamicPropertySource}), nhưng vì là interface method nên khai báo {@code default}.
     * Để dùng trong {@code static} context, bạn cần cast hoặc gọi qua
     * {@link WireMockExtension#STORE_KEY} trực tiếp. Xem ví dụ trong README.
     */
    default WireMockServerRegistry getWireMockRegistry() {
        return WireMockContextHolder.get();
    }

    /**
     * Override {@link YamlTestContext#wireMockRegistry()} — trả về registry từ Extension Store.
     * Nhờ đó {@code YamlTestExecutor} tự lấy đúng registry mà không cần code thêm.
     */
    @Override
    default WireMockServerRegistry wireMockRegistry() {
        return WireMockContextHolder.get();
    }
}
