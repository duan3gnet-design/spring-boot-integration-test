package com.spring.test.yaml.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExpectedResponseSpec {

    private int status = 200;
    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, Object> jsonPath = new LinkedHashMap<>();
    private Object body;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? headers : new LinkedHashMap<>();
    }

    public Map<String, Object> getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(Map<String, Object> jsonPath) {
        this.jsonPath = jsonPath != null ? jsonPath : new LinkedHashMap<>();
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
