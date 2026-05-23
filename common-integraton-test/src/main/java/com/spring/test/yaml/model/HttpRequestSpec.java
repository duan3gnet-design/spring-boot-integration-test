package com.spring.test.yaml.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpRequestSpec {

    private String method = "GET";
    private String path;
    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, String> queryParams = new LinkedHashMap<>();
    private Object body;
    /**
     * Đường dẫn tới file JSON chứa request body (classpath hoặc đường dẫn tương đối).
     * Nếu được khai báo, nội dung file sẽ được load và gán vào {@code body} khi đọc YAML.
     * Ví dụ: {@code bodyFile: requests/create-user.json}
     */
    private String bodyFile;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? headers : new LinkedHashMap<>();
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams != null ? queryParams : new LinkedHashMap<>();
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public String getBodyFile() {
        return bodyFile;
    }

    public void setBodyFile(String bodyFile) {
        this.bodyFile = bodyFile;
    }
}
