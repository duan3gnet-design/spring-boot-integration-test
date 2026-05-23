package com.spring.test.yaml.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Định nghĩa một HTTP stub cho WireMock.
 *
 * <p>Ví dụ YAML:
 * <pre>
 * wireMocks:
 *   - server: payment-service        # tên server đăng ký trong WireMockServerRegistry
 *     stubs:
 *       - method: POST
 *         url: /api/payments
 *         requestBody:               # optional — khớp request body (substring)
 *           contains: "amount"
 *         requestHeaders:            # optional — khớp request header
 *           Content-Type: application/json
 *         status: 200
 *         responseBody:             # inline response body
 *           paymentId: PAY-001
 *           status: SUCCESS
 *         responseBodyFile: wiremock/payment-success.json   # hoặc dùng file
 *         responseHeaders:
 *           Content-Type: application/json
 * </pre>
 */
public class WireMockStub {

    // ── Request matching ──────────────────────────────────────────────────

    /** HTTP method: GET, POST, PUT, PATCH, DELETE, ANY (default: ANY). */
    private String method = "ANY";

    /**
     * URL matching — chọn một trong ba (ưu tiên theo thứ tự):
     * - {@code url}        : khớp chính xác
     * - {@code urlPattern} : regex
     * - {@code urlPath}    : prefix/path (không quan tâm query string)
     */
    private String url;
    private String urlPattern;
    private String urlPath;

    /** Khớp request headers — key: tên header, value: giá trị cần chứa (contains). */
    private Map<String, String> requestHeaders = new LinkedHashMap<>();

    /**
     * Khớp request body — map với key đặc biệt:
     * - {@code contains}: body chứa chuỗi
     * - {@code matches} : body khớp regex
     * - {@code equalTo} : body bằng chính xác
     */
    private Map<String, String> requestBody = new LinkedHashMap<>();

    // ── Response ─────────────────────────────────────────────────────────

    /** HTTP status trả về (default: 200). */
    private int status = 200;

    /** Response body inline (Object — map/list/primitive → serialize thành JSON). */
    private Object responseBody;

    /**
     * Đường dẫn file JSON chứa response body (classpath hoặc relative).
     * Nếu được khai báo, nội dung file sẽ được load và gán vào {@code responseBody}.
     */
    private String responseBodyFile;

    /** Response headers trả về. */
    private Map<String, String> responseHeaders = new LinkedHashMap<>();

    /** Delay phản hồi (milliseconds) — dùng để test timeout. */
    private int delayMs = 0;

    // ── Getters / Setters ─────────────────────────────────────────────────

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUrlPattern() { return urlPattern; }
    public void setUrlPattern(String urlPattern) { this.urlPattern = urlPattern; }

    public String getUrlPath() { return urlPath; }
    public void setUrlPath(String urlPath) { this.urlPath = urlPath; }

    public Map<String, String> getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders != null ? requestHeaders : new LinkedHashMap<>();
    }

    public Map<String, String> getRequestBody() { return requestBody; }
    public void setRequestBody(Map<String, String> requestBody) {
        this.requestBody = requestBody != null ? requestBody : new LinkedHashMap<>();
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public Object getResponseBody() { return responseBody; }
    public void setResponseBody(Object responseBody) { this.responseBody = responseBody; }

    public String getResponseBodyFile() { return responseBodyFile; }
    public void setResponseBodyFile(String responseBodyFile) { this.responseBodyFile = responseBodyFile; }

    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders != null ? responseHeaders : new LinkedHashMap<>();
    }

    public int getDelayMs() { return delayMs; }
    public void setDelayMs(int delayMs) { this.delayMs = delayMs; }

    /** Trả về URL pattern đầu tiên được khai báo (url > urlPattern > urlPath). */
    public String resolvedUrl() {
        if (url != null && !url.isBlank()) return url;
        if (urlPattern != null && !urlPattern.isBlank()) return urlPattern;
        if (urlPath != null && !urlPath.isBlank()) return urlPath;
        return null;
    }
}
