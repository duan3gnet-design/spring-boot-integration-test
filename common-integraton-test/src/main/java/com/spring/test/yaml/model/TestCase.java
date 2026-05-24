package com.spring.test.yaml.model;

import java.util.ArrayList;
import java.util.List;

public class TestCase {

    private String name;
    private String description;
    private boolean enabled = true;
    private List<MockSpec> mocks = new ArrayList<>();
    private List<WireMockSpec> wireMocks = new ArrayList<>();
    private List<DbVerifySpec> dbVerify = new ArrayList<>();
    private HttpRequestSpec request = new HttpRequestSpec();
    private ExpectedResponseSpec expected = new ExpectedResponseSpec();
    /** Nhiều bước HTTP tuần tự trong một test case. */
    private List<TestStep> steps = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<MockSpec> getMocks() {
        return mocks;
    }

    public void setMocks(List<MockSpec> mocks) {
        this.mocks = mocks != null ? mocks : new ArrayList<>();
    }

    public List<WireMockSpec> getWireMocks() {
        return wireMocks;
    }

    public void setWireMocks(List<WireMockSpec> wireMocks) {
        this.wireMocks = wireMocks != null ? wireMocks : new ArrayList<>();
    }

    public List<DbVerifySpec> getDbVerify() {
        return dbVerify;
    }

    public void setDbVerify(List<DbVerifySpec> dbVerify) {
        this.dbVerify = dbVerify != null ? dbVerify : new ArrayList<>();
    }

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

    public List<TestStep> getSteps() {
        return steps;
    }

    public void setSteps(List<TestStep> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public boolean hasSteps() {
        return steps != null && !steps.isEmpty();
    }

    public String displayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (request != null && request.getPath() != null) {
            return request.getMethod() + " " + request.getPath();
        }
        return "unnamed-test";
    }
}
