package com.spring.test.yaml;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.spring.test.yaml.model.WireMockSpec;
import com.spring.test.yaml.model.WireMockStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Apply và reset WireMock stubs từ định nghĩa YAML ({@link WireMockSpec}).
 *
 * <p>Mỗi stub được register vào {@link WireMockServer} tương ứng.
 * Sau khi test xong, gọi {@link #resetStubs(List)} để xóa stub đã đăng ký.
 */
public class YamlWireMockConfigurer {

    private static final Logger log = LoggerFactory.getLogger(YamlWireMockConfigurer.class);

    private final WireMockServerRegistry registry;
    private final JsonMapper jsonMapper;

    public YamlWireMockConfigurer(WireMockServerRegistry registry, JsonMapper jsonMapper) {
        this.registry = registry;
        this.jsonMapper = jsonMapper;
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void applyStubs(List<WireMockSpec> wireMocks) {
        if (wireMocks == null || wireMocks.isEmpty() || registry.isEmpty()) {
            return;
        }
        for (WireMockSpec spec : wireMocks) {
            WireMockServer server = registry.get(spec.getServer());
            for (WireMockStub stub : spec.getStubs()) {
                applyStub(server, stub, spec.getServer());
            }
        }
    }

    public void resetStubs(List<WireMockSpec> wireMocks) {
        if (wireMocks == null || wireMocks.isEmpty() || registry.isEmpty()) {
            return;
        }
        for (WireMockSpec spec : wireMocks) {
            try {
                WireMockServer server = registry.get(spec.getServer());
                server.resetMappings();
                log.debug("Reset WireMock stubs for server '{}'", spec.getServer());
            } catch (Exception e) {
                log.debug("Skip reset WireMock server '{}': {}", spec.getServer(), e.getMessage());
            }
        }
    }

    // ── Stub building ─────────────────────────────────────────────────────

    private void applyStub(WireMockServer server, WireMockStub stub, String serverName) {
        MappingBuilder mapping = buildRequestMapping(stub);
        mapping.willReturn(buildResponse(stub));
        server.stubFor(mapping);
        log.debug("WireMock stub registered on '{}': {} {}",
                serverName, stub.getMethod(), stub.resolvedUrl());
    }

    private MappingBuilder buildRequestMapping(WireMockStub stub) {
        MappingBuilder mapping = buildUrlMapping(stub);

        // Request headers matching
        for (Map.Entry<String, String> header : stub.getRequestHeaders().entrySet()) {
            mapping.withHeader(header.getKey(), containing(header.getValue()));
        }

        // Request body matching
        for (Map.Entry<String, String> bodyMatcher : stub.getRequestBody().entrySet()) {
            StringValuePattern pattern = switch (bodyMatcher.getKey()) {
                case "matches"  -> matching(bodyMatcher.getValue());
                case "equalTo"  -> equalTo(bodyMatcher.getValue());
                default         -> containing(bodyMatcher.getValue()); // "contains" atau key apapun
            };
            mapping.withRequestBody(pattern);
        }

        return mapping;
    }

    private MappingBuilder buildUrlMapping(WireMockStub stub) {
        String method = stub.getMethod().toUpperCase();
        // url (exact) > urlPattern (regex) > urlPath (path only)
        if (stub.getUrl() != null && !stub.getUrl().isBlank()) {
            return switch (method) {
                case "GET"    -> get(urlEqualTo(stub.getUrl()));
                case "POST"   -> post(urlEqualTo(stub.getUrl()));
                case "PUT"    -> put(urlEqualTo(stub.getUrl()));
                case "PATCH"  -> patch(urlEqualTo(stub.getUrl()));
                case "DELETE" -> delete(urlEqualTo(stub.getUrl()));
                default       -> request(method, urlEqualTo(stub.getUrl()));
            };
        }
        if (stub.getUrlPattern() != null && !stub.getUrlPattern().isBlank()) {
            return switch (method) {
                case "GET"    -> get(urlMatching(stub.getUrlPattern()));
                case "POST"   -> post(urlMatching(stub.getUrlPattern()));
                case "PUT"    -> put(urlMatching(stub.getUrlPattern()));
                case "PATCH"  -> patch(urlMatching(stub.getUrlPattern()));
                case "DELETE" -> delete(urlMatching(stub.getUrlPattern()));
                default       -> request(method, urlMatching(stub.getUrlPattern()));
            };
        }
        if (stub.getUrlPath() != null && !stub.getUrlPath().isBlank()) {
            return switch (method) {
                case "GET"    -> get(urlPathEqualTo(stub.getUrlPath()));
                case "POST"   -> post(urlPathEqualTo(stub.getUrlPath()));
                case "PUT"    -> put(urlPathEqualTo(stub.getUrlPath()));
                case "PATCH"  -> patch(urlPathEqualTo(stub.getUrlPath()));
                case "DELETE" -> delete(urlPathEqualTo(stub.getUrlPath()));
                default       -> request(method, urlPathEqualTo(stub.getUrlPath()));
            };
        }
        // Không khai báo URL → match any URL
        return any(WireMock.anyUrl());
    }

    private com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder buildResponse(WireMockStub stub) {
        var response = aResponse().withStatus(stub.getStatus());

        // Response headers
        for (Map.Entry<String, String> header : stub.getResponseHeaders().entrySet()) {
            response.withHeader(header.getKey(), header.getValue());
        }

        // Response body — file > inline object
        String bodyJson = resolveResponseBody(stub);
        if (bodyJson != null) {
            response.withBody(bodyJson);
            // Tự động thêm Content-Type nếu chưa có
            if (!stub.getResponseHeaders().containsKey("Content-Type")) {
                response.withHeader("Content-Type", "application/json");
            }
        }

        // Delay
        if (stub.getDelayMs() > 0) {
            response.withFixedDelay(stub.getDelayMs());
        }

        return response;
    }

    private String resolveResponseBody(WireMockStub stub) {
        // Ưu tiên responseBodyFile
        if (stub.getResponseBodyFile() != null && !stub.getResponseBodyFile().isBlank()) {
            return loadJsonFile(stub.getResponseBodyFile());
        }
        // Inline responseBody object
        if (stub.getResponseBody() != null) {
            return jsonMapper.writeValueAsString(stub.getResponseBody());
        }
        return null;
    }

    private String loadJsonFile(String filePath) {
        String normalized = filePath.startsWith("/") ? filePath : "/" + filePath;
        InputStream in = YamlWireMockConfigurer.class.getResourceAsStream(normalized);
        if (in == null) {
            throw new IllegalArgumentException("WireMock responseBodyFile not found on classpath: " + filePath);
        }
        try (in) {
            return new String(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read WireMock responseBodyFile: " + filePath, e);
        }
    }
}
