package com.spring.test.yaml.junit;

import com.spring.test.yaml.WireMockServerRegistry;

/**
 * ThreadLocal holder — cầu nối giữa {@link WireMockExtension} và {@link WireMockSupport}.
 *
 * <p>{@link WireMockExtension} ghi registry vào đây sau khi start server ({@code BeforeAll}).
 * {@link WireMockSupport} đọc ra để inject vào {@code YamlTestExecutor} và
 * {@code @DynamicPropertySource}.
 *
 * <p>Không dùng trực tiếp — chỉ dành cho internal use.
 */
final class WireMockContextHolder {

    private static final ThreadLocal<WireMockServerRegistry> HOLDER = new ThreadLocal<>();

    private WireMockContextHolder() {}

    static void set(WireMockServerRegistry registry) {
        HOLDER.set(registry);
    }

    static WireMockServerRegistry get() {
        WireMockServerRegistry registry = HOLDER.get();
        if (registry == null) {
            throw new IllegalStateException(
                    "WireMockServerRegistry not initialized. " +
                    "Make sure the test class is annotated with @EnableWireMock.");
        }
        return registry;
    }

    static void clear() {
        HOLDER.remove();
    }
}
