package com.spring.test.yaml.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Nhóm các WireMock stub cho một server cụ thể.
 *
 * <p>Ví dụ YAML:
 * <pre>
 * wireMocks:
 *   - server: payment-service
 *     stubs:
 *       - method: POST
 *         url: /api/payments
 *         status: 200
 *         responseBody:
 *           paymentId: PAY-001
 * </pre>
 */
public class WireMockSpec {

    /**
     * Tên WireMock server — khớp với key đăng ký trong {@code WireMockServerRegistry}.
     * Nếu chỉ có một server, có thể bỏ qua (dùng server mặc định).
     */
    private String server;

    private List<WireMockStub> stubs = new ArrayList<>();

    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }

    public List<WireMockStub> getStubs() { return stubs; }
    public void setStubs(List<WireMockStub> stubs) {
        this.stubs = stubs != null ? stubs : new ArrayList<>();
    }
}
