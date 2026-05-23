package com.spring.test.yaml.junit;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kích hoạt WireMock cho test class — tự động start/stop/reset server.
 *
 * <p>Extension sẽ:
 * <ul>
 *   <li>Start tất cả server trước khi chạy class ({@code BeforeAll})</li>
 *   <li>Reset tất cả mapping trước mỗi test ({@code BeforeEach})</li>
 *   <li>Stop tất cả server sau khi chạy xong class ({@code AfterAll})</li>
 *   <li>Đăng ký {@link com.spring.test.yaml.WireMockServerRegistry} vào JUnit Store
 *       để test class lấy qua {@link WireMockSupport}</li>
 * </ul>
 *
 * <p>Ví dụ:
 * <pre>
 * &#64;SpringBootTest
 * &#64;AutoConfigureMockMvc
 * &#64;EnableWireMock({
 *     &#64;WireMockServer(name = "payment-service"),
 *     &#64;WireMockServer(name = "notification-service")
 * })
 * class MyTest implements YamlTestContext, YamlIntegrationTestSupport, WireMockSupport {
 *
 *     &#64;DynamicPropertySource
 *     static void wireMockProperties(DynamicPropertyRegistry registry) {
 *         // Lấy port từ registry sau khi server đã start
 *         registry.add("payment.url",
 *             () -> "http://localhost:" + getWireMockRegistry().get("payment-service").port());
 *     }
 * }
 * </pre>
 *
 * <p>Nếu chỉ có một server, có thể bỏ {@code name}:
 * <pre>
 * &#64;EnableWireMock(@WireMockServer)
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(WireMockExtension.class)
public @interface EnableWireMock {

    /** Danh sách server cần khởi tạo. */
    WireMockServer[] value();
}
