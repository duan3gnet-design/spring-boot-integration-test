package com.spring.test.yaml.junit;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.spring.test.yaml.WireMockServerRegistry;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 Extension — tự động quản lý vòng đời WireMock server.
 *
 * <p>Được kích hoạt qua {@link EnableWireMock}. Không cần dùng trực tiếp.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>{@code BeforeAll} — khởi tạo và start tất cả server, lưu registry vào JUnit Store</li>
 *   <li>{@code BeforeEach} — reset tất cả mapping (stub sạch trước mỗi test)</li>
 *   <li>{@code AfterAll} — stop tất cả server</li>
 * </ul>
 */
public class WireMockExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private static final Logger log = LoggerFactory.getLogger(WireMockExtension.class);

    /** Key lưu registry trong JUnit ExtensionContext.Store. */
    static final String STORE_KEY = "wireMockServerRegistry";

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(WireMockExtension.class);

    // ── BeforeAll ─────────────────────────────────────────────────────────

    @Override
    public void beforeAll(ExtensionContext context) {
        EnableWireMock annotation = AnnotationSupport
                .findAnnotation(context.getRequiredTestClass(), EnableWireMock.class)
                .orElseThrow(() -> new IllegalStateException(
                        "@EnableWireMock not found on " + context.getRequiredTestClass().getName()));

        WireMockServerRegistry registry = new WireMockServerRegistry();

        for (WireMockServer serverDef : annotation.value()) {
            com.github.tomakehurst.wiremock.WireMockServer server = buildServer(serverDef);
            server.start();

            String name = serverDef.name();
            if (name == null || name.isBlank()) {
                registry.registerDefault(server);
                log.info("WireMock default server started on port {}", server.port());
            } else {
                registry.register(name, server);
                log.info("WireMock server '{}' started on port {}", name, server.port());
            }
        }

        // Lưu vào store gắn với class (tồn tại suốt vòng đời test class)
        store(context).put(STORE_KEY, registry);
        // Ghi vào ThreadLocal để WireMockSupport và @DynamicPropertySource truy cập
        WireMockContextHolder.set(registry);
    }

    // ── BeforeEach ────────────────────────────────────────────────────────

    @Override
    public void beforeEach(ExtensionContext context) {
        // Reset stub trước mỗi test — tránh stub rò giữa các case
        WireMockServerRegistry registry = getRegistry(context);
        if (registry != null) {
            registry.resetAll();
        }
    }

    // ── AfterAll ──────────────────────────────────────────────────────────

    @Override
    public void afterAll(ExtensionContext context) {
        WireMockServerRegistry registry = getRegistry(context);
        if (registry != null) {
            registry.stopAll();
            log.info("WireMock servers stopped for {}", context.getRequiredTestClass().getSimpleName());
        }
        WireMockContextHolder.clear();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private com.github.tomakehurst.wiremock.WireMockServer buildServer(WireMockServer def) {
        WireMockConfiguration config = WireMockConfiguration.wireMockConfig();
        if (def.port() == 0) {
            config.dynamicPort();
        } else {
            config.port(def.port());
        }
        return new com.github.tomakehurst.wiremock.WireMockServer(config);
    }

    private ExtensionContext.Store store(ExtensionContext context) {
        // Dùng root store để có thể truy cập từ cả BeforeAll (static) lẫn BeforeEach (instance)
        return context.getRoot().getStore(NAMESPACE);
    }

    private WireMockServerRegistry getRegistry(ExtensionContext context) {
        return store(context).get(STORE_KEY, WireMockServerRegistry.class);
    }
}
