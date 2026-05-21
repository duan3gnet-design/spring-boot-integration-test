package com.spring.test.yaml.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestStep {

    private HttpRequestSpec request = new HttpRequestSpec();
    private ExpectedResponseSpec expected = new ExpectedResponseSpec();
    /** Lưu giá trị từ response: tên biến → jsonPath (ví dụ id: $.id). */
    private Map<String, String> capture = new LinkedHashMap<>();

    public HttpRequestSpec getRequest() {
        return request;
    }

    public void setRequest(HttpRequestSpec request) {
        this.request = request != null ? request : new HttpRequestSpec();
    }

    public ExpectedResponseSpec getExpected() {
        return expected;
    }

    public void setExpected(ExpectedResponseSpec expected) {
        this.expected = expected != null ? expected : new ExpectedResponseSpec();
    }

    public Map<String, String> getCapture() {
        return capture;
    }

    public void setCapture(Map<String, String> capture) {
        this.capture = capture != null ? capture : new LinkedHashMap<>();
    }
}
