package com.spring.test.yaml.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Định nghĩa mock/stub cho một Spring bean.
 */
public class MockSpec {

    /** Tên bean trong Spring context (ví dụ transactionRepository). */
    private String bean;

    /** FQCN — dùng khi không có bean name. */
    private String type;

    private List<MockStub> stubs = new ArrayList<>();

    public String getBean() {
        return bean;
    }

    public void setBean(String bean) {
        this.bean = bean;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<MockStub> getStubs() {
        return stubs;
    }

    public void setStubs(List<MockStub> stubs) {
        this.stubs = stubs != null ? stubs : new ArrayList<>();
    }
}
