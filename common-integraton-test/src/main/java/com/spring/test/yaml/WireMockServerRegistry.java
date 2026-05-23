package com.spring.test.yaml;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry lưu trữ các {@link WireMockServer} theo tên.
 *
 * <p>Dùng trong test class để đăng ký server và inject vào {@link YamlTestContext}:
 * <pre>
 * private final WireMockServerRegistry wireMockRegistry = new WireMockServerRegistry();
 *
 * &#64;BeforeAll
 * static void startWireMock() {
 *     wireMockRegistry.register("payment-service", new WireMockServer(wireMockConfig().dynamicPort()));
 *     wireMockRegistry.startAll();
 * }
 *
 * &#64;AfterAll
 * static void stopWireMock() {
 *     wireMockRegistry.stopAll();
 * }
 *
 * &#64;Override
 * public WireMockServerRegistry wireMockRegistry() {
 *     return wireMockRegistry;
 * }
 * </pre>
 */
public class WireMockServerRegistry {

    private static final String DEFAULT = "__default__";

    private final Map<String, WireMockServer> servers = new LinkedHashMap<>();

    // ── Registration ──────────────────────────────────────────────────────

    /** Đăng ký server theo tên — tên này phải khớp với {@code wireMocks[].server} trong YAML. */
    public WireMockServerRegistry register(String name, WireMockServer server) {
        servers.put(name, server);
        return this;
    }

    /**
     * Đăng ký server mặc định — dùng khi YAML không khai báo {@code server}.
     * Hoặc khi chỉ có một WireMock server duy nhất.
     */
    public WireMockServerRegistry registerDefault(WireMockServer server) {
        servers.put(DEFAULT, server);
        return this;
    }

    // ── Lookup ────────────────────────────────────────────────────────────

    /**
     * Lấy server theo tên.
     * Nếu {@code name} là null/blank → trả về default server.
     */
    public WireMockServer get(String name) {
        if (name == null || name.isBlank()) {
            WireMockServer def = servers.get(DEFAULT);
            if (def == null) {
                if (servers.size() == 1) {
                    return servers.values().iterator().next();
                }
                throw new IllegalStateException(
                        "No default WireMock server registered. " +
                        "Use registerDefault() or specify 'server' name in YAML.");
            }
            return def;
        }
        WireMockServer server = servers.get(name);
        if (server == null) {
            throw new IllegalStateException(
                    "WireMock server not registered: '" + name + "'. " +
                    "Available: " + servers.keySet());
        }
        return server;
    }

    public boolean isEmpty() {
        return servers.isEmpty();
    }

    public Map<String, WireMockServer> all() {
        return Collections.unmodifiableMap(servers);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public void startAll() {
        servers.values().forEach(s -> {
            if (!s.isRunning()) s.start();
        });
    }

    public void stopAll() {
        servers.values().forEach(s -> {
            if (s.isRunning()) s.stop();
        });
    }

    public void resetAll() {
        servers.values().forEach(WireMockServer::resetAll);
    }
}
