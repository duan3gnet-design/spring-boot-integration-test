package com.spring.test.yaml;

import com.spring.test.yaml.model.ExpectedResponseSpec;
import com.spring.test.yaml.model.HttpRequestSpec;
import com.spring.test.yaml.model.TestCase;
import com.spring.test.yaml.model.TestStep;
import com.spring.test.yaml.model.TestSuite;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Đọc file YAML (classpath hoặc filesystem) thành {@link TestSuite}.
 *
 * <p>Sau khi parse YAML, tự động resolve {@code bodyFile} trong
 * {@link HttpRequestSpec} và {@link ExpectedResponseSpec}:
 * nếu field {@code bodyFile} được khai báo, nội dung JSON sẽ được load
 * và gán vào field {@code body} tương ứng.
 *
 * <p>Quy tắc tìm file JSON (theo thứ tự ưu tiên):
 * <ol>
 *   <li>Classpath tuyệt đối: {@code /the/path.json}</li>
 *   <li>Classpath tương đối cùng thư mục với file YAML gốc</li>
 *   <li>Classpath tương đối từ root ({@code /path.json})</li>
 * </ol>
 */
public final class YamlTestDataReader {

    private final YAMLMapper yamlMapper;
    private final JsonMapper jsonMapper;

    public YamlTestDataReader() {
        this.yamlMapper = YAMLMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        this.jsonMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public YamlTestDataReader(YAMLMapper yamlMapper) {
        this.yamlMapper = yamlMapper;
        this.jsonMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public YamlTestDataReader(YAMLMapper yamlMapper, JsonMapper jsonMapper) {
        this.yamlMapper = yamlMapper;
        this.jsonMapper = jsonMapper;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public TestSuite loadClasspath(String classpathLocation) {
        String normalized = classpathLocation.startsWith("/") ? classpathLocation : "/" + classpathLocation;
        InputStream in = YamlTestDataReader.class.getResourceAsStream(normalized);
        if (in == null) {
            throw new IllegalArgumentException("YAML not found on classpath: " + classpathLocation);
        }
        try (in) {
            TestSuite suite = yamlMapper.readValue(in, TestSuite.class);
            resolveBodyFiles(suite, normalized);
            return suite;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read YAML: " + classpathLocation, e);
        }
    }

    public TestSuite loadFile(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            TestSuite suite = yamlMapper.readValue(in, TestSuite.class);
            resolveBodyFilesFromPath(suite, path);
            return suite;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read YAML: " + path, e);
        }
    }

    public List<TestCase> loadEnabledCases(String classpathLocation) {
        return loadClasspath(classpathLocation).getTests().stream()
                .filter(TestCase::isEnabled)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Body-file resolution — classpath variant
    // -------------------------------------------------------------------------

    /**
     * Duyệt toàn bộ TestCase và TestStep, resolve {@code bodyFile} → {@code body}.
     *
     * @param suite            suite vừa parse
     * @param originClasspath  đường dẫn classpath của file YAML gốc (ví dụ {@code /tests/suite.yaml})
     */
    private void resolveBodyFiles(TestSuite suite, String originClasspath) {
        // Thư mục chứa file YAML, dùng làm base cho bodyFile tương đối
        String baseDir = classpathParentDir(originClasspath);

        for (TestCase testCase : suite.getTests()) {
            resolveRequest(testCase.getRequest(), baseDir, testCase.getName());
            resolveExpected(testCase.getExpected(), baseDir, testCase.getName());
            for (TestStep step : testCase.getSteps()) {
                resolveRequest(step.getRequest(), baseDir, testCase.getName());
                resolveExpected(step.getExpected(), baseDir, testCase.getName());
            }
        }
    }

    /** Giải quyết bodyFile khi load từ Path filesystem. */
    private void resolveBodyFilesFromPath(TestSuite suite, Path yamlPath) {
        Path baseDir = yamlPath.getParent();
        for (TestCase testCase : suite.getTests()) {
            resolveRequestFromPath(testCase.getRequest(), baseDir, testCase.getName());
            resolveExpectedFromPath(testCase.getExpected(), baseDir, testCase.getName());
            for (TestStep step : testCase.getSteps()) {
                resolveRequestFromPath(step.getRequest(), baseDir, testCase.getName());
                resolveExpectedFromPath(step.getExpected(), baseDir, testCase.getName());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Per-spec helpers — classpath
    // -------------------------------------------------------------------------

    private void resolveRequest(HttpRequestSpec spec, String baseDir, String testName) {
        if (spec == null || spec.getBodyFile() == null || spec.getBodyFile().isBlank()) {
            return;
        }
        if (spec.getBody() != null) {
            throw new IllegalStateException(
                    "Test '" + testName + "': request defines both 'body' and 'bodyFile' — use only one.");
        }
        spec.setBody(loadJsonFromClasspath(spec.getBodyFile(), baseDir, testName + " request.bodyFile"));
        spec.setBodyFile(null); // consumed
    }

    private void resolveExpected(ExpectedResponseSpec spec, String baseDir, String testName) {
        if (spec == null || spec.getBodyFile() == null || spec.getBodyFile().isBlank()) {
            return;
        }
        if (spec.getBody() != null) {
            throw new IllegalStateException(
                    "Test '" + testName + "': expected defines both 'body' and 'bodyFile' — use only one.");
        }
        spec.setBody(loadJsonFromClasspath(spec.getBodyFile(), baseDir, testName + " expected.bodyFile"));
        spec.setBodyFile(null); // consumed
    }

    // -------------------------------------------------------------------------
    // Per-spec helpers — filesystem Path
    // -------------------------------------------------------------------------

    private void resolveRequestFromPath(HttpRequestSpec spec, Path baseDir, String testName) {
        if (spec == null || spec.getBodyFile() == null || spec.getBodyFile().isBlank()) {
            return;
        }
        if (spec.getBody() != null) {
            throw new IllegalStateException(
                    "Test '" + testName + "': request defines both 'body' and 'bodyFile' — use only one.");
        }
        spec.setBody(loadJsonFromPath(spec.getBodyFile(), baseDir, testName + " request.bodyFile"));
        spec.setBodyFile(null);
    }

    private void resolveExpectedFromPath(ExpectedResponseSpec spec, Path baseDir, String testName) {
        if (spec == null || spec.getBodyFile() == null || spec.getBodyFile().isBlank()) {
            return;
        }
        if (spec.getBody() != null) {
            throw new IllegalStateException(
                    "Test '" + testName + "': expected defines both 'body' and 'bodyFile' — use only one.");
        }
        spec.setBody(loadJsonFromPath(spec.getBodyFile(), baseDir, testName + " expected.bodyFile"));
        spec.setBodyFile(null);
    }

    // -------------------------------------------------------------------------
    // JSON loading utilities
    // -------------------------------------------------------------------------

    /**
     * Load JSON từ classpath, ưu tiên:
     * 1. Nếu {@code filePath} bắt đầu bằng "/" → tuyệt đối trên classpath
     * 2. Relative so với thư mục chứa YAML gốc
     * 3. Relative từ root classpath (/filePath)
     */
    private Object loadJsonFromClasspath(String filePath, String baseDir, String context) {
        // Thử tuyệt đối trước
        if (filePath.startsWith("/")) {
            return readJsonResource(filePath, context);
        }
        // Thử relative từ cùng thư mục YAML
        String relPath = baseDir + filePath;
        InputStream in = YamlTestDataReader.class.getResourceAsStream(relPath);
        if (in != null) {
            try (in) {
                return jsonMapper.readValue(in, Object.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read bodyFile '" + filePath + "' [" + context + "]", e);
            }
        }
        // Thử từ root classpath
        return readJsonResource("/" + filePath, context);
    }

    private Object readJsonResource(String classpathPath, String context) {
        InputStream in = YamlTestDataReader.class.getResourceAsStream(classpathPath);
        if (in == null) {
            throw new IllegalArgumentException(
                    "bodyFile not found on classpath: '" + classpathPath + "' [" + context + "]");
        }
        try (in) {
            return jsonMapper.readValue(in, Object.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read bodyFile '" + classpathPath + "' [" + context + "]", e);
        }
    }

    /** Load JSON từ filesystem — resolve tương đối so với thư mục chứa YAML. */
    private Object loadJsonFromPath(String filePath, Path baseDir, String context) {
        Path resolved = (baseDir != null)
                ? baseDir.resolve(filePath).normalize()
                : Path.of(filePath);
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException(
                    "bodyFile not found: '" + resolved + "' [" + context + "]");
        }
        try (InputStream in = Files.newInputStream(resolved)) {
            return jsonMapper.readValue(in, Object.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read bodyFile '" + resolved + "' [" + context + "]", e);
        }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /**
     * Trả về thư mục chứa file YAML trên classpath, đảm bảo kết thúc bằng "/".
     * Ví dụ: {@code /tests/api/suite.yaml} → {@code /tests/api/}
     */
    private static String classpathParentDir(String classpathLocation) {
        int lastSlash = classpathLocation.lastIndexOf('/');
        return lastSlash >= 0 ? classpathLocation.substring(0, lastSlash + 1) : "/";
    }
}
