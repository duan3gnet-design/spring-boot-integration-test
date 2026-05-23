package com.spring.test.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.spring.test.yaml.model.ExpectedResponseSpec;
import com.spring.test.yaml.model.HttpRequestSpec;
import com.spring.test.yaml.model.TestCase;
import com.spring.test.yaml.model.TestStep;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Thực thi một {@link TestCase} qua MockMvc và assert kết quả theo YAML.
 * Không dùng Hamcrest — assert trực tiếp trên {@link MvcResult}.
 */
public class YamlMockMvcTestRunner {

    private static final Logger log = LoggerFactory.getLogger(YamlMockMvcTestRunner.class);
    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;

    public YamlMockMvcTestRunner(MockMvc mockMvc, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
    }

    public void run(TestCase testCase) throws Exception {
        Map<String, Object> variables = new HashMap<>();
        if (testCase.hasSteps()) {
            for (TestStep step : testCase.getSteps()) {
                runStep(testCase.displayName(), step, variables);
            }
            return;
        }
        runStep(testCase.displayName(), testCase.getRequest(), testCase.getExpected(), Map.of(), variables);
    }

    private void runStep(String testName, TestStep step, Map<String, Object> variables) throws Exception {
        runStep(testName, step.getRequest(), step.getExpected(), step.getCapture(), variables);
    }

    private void runStep(String testName, HttpRequestSpec request, ExpectedResponseSpec expected,
                         Map<String, String> capture, Map<String, Object> variables) throws Exception {
        HttpRequestSpec resolved = resolveRequest(request, variables);
        if (resolved.getPath() == null || resolved.getPath().isBlank()) {
            throw new IllegalArgumentException("Test '" + testName + "' requires request.path");
        }

        MvcResult mvcResult = mockMvc.perform(buildRequest(resolved)).andReturn();
        assertResponse(mvcResult, expected);
        captureVariables(capture, variables, mvcResult);
    }

    private void assertResponse(MvcResult mvcResult, ExpectedResponseSpec expected) throws Exception {
        int actualStatus = mvcResult.getResponse().getStatus();
        if (actualStatus != expected.getStatus()) {
            throw new Exception("Expected status " + expected.getStatus() + " but was " + actualStatus
                    + ". Body: " + mvcResult.getResponse().getContentAsString());
        }

        for (Map.Entry<String, String> header : expected.getHeaders().entrySet()) {
            String actual = mvcResult.getResponse().getHeader(header.getKey());
            assertHeaderValue(header.getKey(), header.getValue(), actual);
        }

        String body = mvcResult.getResponse().getContentAsString();
        JsonNode root = jsonMapper.readTree(body.isBlank() ? "{}" : body);

        for (Map.Entry<String, Object> jsonPath : expected.getJsonPath().entrySet()) {
            assertJsonPathValue(root, jsonPath.getKey(), jsonPath.getValue());
        }

        if (expected.getBody() != null) {
            JsonNode expectedNode = jsonMapper.valueToTree(expected.getBody());
            assertBodyPartialMatch(root, expectedNode);
        }
    }

    /**
     * So khớp body theo kiểu partial match:
     * - Duyệt từng field trong {@code expected} (object JSON)
     * - Field có giá trị {@code null} hoặc {@code "$exists"} → chỉ kiểm tra field tồn tại và không null
     * - Field có giá trị {@code "$notNull"} → kiểm tra tồn tại, không null, không blank
     * - Field có giá trị khác → so khớp chính xác với actual
     * - Các field trong actual nhưng không có trong expected → bỏ qua (partial)
     */
    private void assertBodyPartialMatch(JsonNode actual, JsonNode expected) throws Exception {
        if (expected.isObject()) {
            for (var entry : expected.properties()) {
                String fieldName = entry.getKey();
                JsonNode expectedVal = entry.getValue();
                JsonNode actualVal = actual.get(fieldName);

                if (expectedVal.isNull()) {
                    // null trong expected → chỉ yêu cầu field tồn tại
                    if (actualVal == null || actualVal.isMissingNode()) {
                        throw new Exception("Body field '" + fieldName + "' not present in response");
                    }
                } else if (expectedVal.isString()) {
                    String text = expectedVal.asString();
                    if (text.equals("$exists")) {
                        if (actualVal == null || actualVal.isMissingNode() || actualVal.isNull()) {
                            throw new Exception("Body field '" + fieldName + "' expected to exist but was absent or null");
                        }
                    } else if (text.equals("$notNull")) {
                        if (actualVal == null || actualVal.isMissingNode() || actualVal.isNull()
                                || (actualVal.isString() && actualVal.asString().isBlank())) {
                            throw new Exception("Body field '" + fieldName + "' expected to be non-null/non-blank");
                        }
                    } else if (text.startsWith("regex:")) {
                        String pattern = text.substring("regex:".length());
                        String actualText = actualVal != null && actualVal.isString()
                                ? actualVal.asString() : (actualVal != null ? actualVal.toString() : "");
                        if (!Pattern.compile(pattern).matcher(actualText).matches()) {
                            throw new Exception("Body field '" + fieldName + "' expected regex '" + pattern
                                    + "' but was: " + actualText);
                        }
                    } else if (text.startsWith("contains:")) {
                        String fragment = text.substring("contains:".length());
                        String actualText = actualVal != null && actualVal.isString()
                                ? actualVal.asString() : (actualVal != null ? actualVal.toString() : "");
                        if (!actualText.contains(fragment)) {
                            throw new Exception("Body field '" + fieldName + "' expected to contain '" + fragment
                                    + "' but was: " + actualText);
                        }
                    } else {
                        if (!expectedVal.equals(actualVal)) {
                            throw new Exception("Body field '" + fieldName + "' expected "
                                    + expectedVal + " but was " + actualVal);
                        }
                    }
                } else if (expectedVal.isObject() && actualVal != null && actualVal.isObject()) {
                    // Đệ quy cho nested object
                    assertBodyPartialMatch(actualVal, expectedVal);
                } else {
                    if (!expectedVal.equals(actualVal)) {
                        throw new Exception("Body field '" + fieldName + "' expected "
                                + expectedVal + " but was " + actualVal);
                    }
                }
            }
        } else {
            // Non-object (array, primitive) → so khớp chính xác
            if (!actual.equals(expected)) {
                throw new Exception("Response body mismatch.\nExpected: " + expected + "\nActual: " + actual);
            }
        }
    }

    private void assertHeaderValue(String name, String expected, String actual) throws Exception {
        if (actual == null) {
            throw new Exception("Header '" + name + "' not present. Expected: " + expected);
        }
        if (expected.startsWith("regex:")) {
            String pattern = expected.substring("regex:".length());
            if (!Pattern.compile(pattern).matcher(actual).find()) {
                throw new Exception("Header '" + name + "' expected to match regex '" + pattern
                        + "' but was: " + actual);
            }
            return;
        }
        if (expected.startsWith("contains:")) {
            String fragment = expected.substring("contains:".length());
            if (!actual.contains(fragment)) {
                throw new Exception("Header '" + name + "' expected to contain '" + fragment
                        + "' but was: " + actual);
            }
            return;
        }
        if (!actual.equals(expected)) {
            throw new Exception("Header '" + name + "' expected '" + expected + "' but was: " + actual);
        }
    }

    private void assertJsonPathValue(JsonNode root, String path, Object expected) throws Exception {
        if (expected instanceof String str) {
            switch (str) {
                case "$exists" -> {
                    if (!nodeExists(root, path)) {
                        throw new Exception("JSON path does not exist: " + path);
                    }
                }
                case "$notNull" -> {
                    JsonNode node = readJsonPath(root, path);
                    if (node == null || node.isNull() || (node.isString() && node.asString().isBlank())) {
                        throw new Exception("JSON path is null/empty: " + path);
                    }
                }
                default -> {
                    JsonNode node = readJsonPath(root, path);
                    if (str.startsWith("regex:")) {
                        String pattern = str.substring("regex:".length());
                        String actual = nodeText(node, path);
                        if (!Pattern.compile(pattern).matcher(actual).matches()) {
                            throw new Exception("JSON path " + path + " expected regex '" + pattern
                                    + "' but was: " + actual);
                        }
                    } else if (str.startsWith("contains:")) {
                        String fragment = str.substring("contains:".length());
                        String actual = nodeText(node, path);
                        if (!actual.contains(fragment)) {
                            throw new Exception("JSON path " + path + " expected to contain '" + fragment
                                    + "' but was: " + actual);
                        }
                    } else {
                        assertJsonValueEquals(path, node, str);
                    }
                }
            }
            return;
        }
        JsonNode node = readJsonPath(root, path);
        assertJsonValueEquals(path, node, expected);
    }

    private void assertJsonValueEquals(String path, JsonNode node, Object expected) throws Exception {
        if (node == null || node.isMissingNode()) {
            throw new Exception("JSON path not found: " + path);
        }
        JsonNode expectedNode = jsonMapper.valueToTree(expected);
        if (!node.equals(expectedNode)) {
            throw new Exception("JSON path " + path + " expected " + expectedNode + " but was " + node);
        }
    }

    private boolean nodeExists(JsonNode root, String path) {
        try {
            JsonNode node = readJsonPath(root, path);
            return node != null && !node.isMissingNode() && !node.isNull();
        } catch (Exception e) {
            return false;
        }
    }

    private String nodeText(JsonNode node, String path) throws Exception {
        if (node == null || node.isMissingNode() || node.isNull()) {
            throw new Exception("JSON path not found: " + path);
        }
        return node.isString() ? node.asString() : node.toString();
    }

    private void captureVariables(Map<String, String> capture, Map<String, Object> variables, MvcResult mvcResult)
            throws Exception {
        if (capture == null || capture.isEmpty()) {
            return;
        }
        String responseBody = mvcResult.getResponse().getContentAsString();
        JsonNode root = jsonMapper.readTree(responseBody.isBlank() ? "{}" : responseBody);
        for (Map.Entry<String, String> entry : capture.entrySet()) {
            JsonNode node = readJsonPath(root, entry.getValue());
            if (node == null || node.isMissingNode() || node.isNull()) {
                throw new IllegalArgumentException(
                        "Capture failed: " + entry.getKey() + " from path " + entry.getValue());
            }
            variables.put(entry.getKey(), node.isNumber() ? node.numberValue() : node.asString());
        }
    }

    private JsonNode readJsonPath(JsonNode root, String path) {
        if (!path.startsWith("$.")) {
            throw new IllegalArgumentException("JSON path must start with $. — got: " + path);
        }
        JsonNode current = root;
        String remainder = path.substring(2);
        if (remainder.isBlank()) {
            return current;
        }
        for (String segment : remainder.split("\\.")) {
            if (current == null || current.isMissingNode()) {
                return null;
            }
            current = navigateSegment(current, segment);
        }
        return current;
    }

    private JsonNode navigateSegment(JsonNode current, String segment) {
        int bracket = segment.indexOf('[');
        if (bracket < 0) {
            return current.get(segment);
        }
        String field = segment.substring(0, bracket);
        int endBracket = segment.indexOf(']', bracket);
        int index = Integer.parseInt(segment.substring(bracket + 1, endBracket));
        JsonNode array = field.isEmpty() ? current : current.get(field);
        if (array == null || !array.isArray()) {
            return null;
        }
        return array.get(index);
    }

    private HttpRequestSpec resolveRequest(HttpRequestSpec request, Map<String, Object> variables) {
        HttpRequestSpec resolved = new HttpRequestSpec();
        resolved.setMethod(request.getMethod());
        resolved.setPath(YamlVariableResolver.resolve(request.getPath(), variables));
        resolved.setHeaders(YamlVariableResolver.resolveMap(request.getHeaders(), variables));
        resolved.setQueryParams(YamlVariableResolver.resolveMap(request.getQueryParams(), variables));
        resolved.setBody(YamlVariableResolver.resolveBody(request.getBody(), variables));
        return resolved;
    }

    private MockHttpServletRequestBuilder buildRequest(HttpRequestSpec request) {
        HttpMethod method = HttpMethod.valueOf(request.getMethod().trim().toUpperCase());
        MockHttpServletRequestBuilder builder = switch (method.name()) {
            case "GET" -> MockMvcRequestBuilders.get(request.getPath());
            case "POST" -> MockMvcRequestBuilders.post(request.getPath());
            case "PUT" -> MockMvcRequestBuilders.put(request.getPath());
            case "PATCH" -> MockMvcRequestBuilders.patch(request.getPath());
            case "DELETE" -> MockMvcRequestBuilders.delete(request.getPath());
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + request.getMethod());
        };

        request.getQueryParams().forEach(builder::param);
        request.getHeaders().forEach(builder::header);

        if (request.getBody() != null) {
            String json = jsonMapper.writeValueAsString(request.getBody());
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        } else if (!request.getHeaders().containsKey("Content-Type")
                && (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)) {
            builder.contentType(MediaType.APPLICATION_JSON);
        }
        return builder;
    }
}
