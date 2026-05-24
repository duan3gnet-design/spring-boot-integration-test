package com.spring.test.yaml;

import com.spring.test.yaml.model.DbVerifySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Verify dữ liệu trong database theo khai báo {@link DbVerifySpec} từ YAML.
 *
 * <p>Dùng JDBC trực tiếp — không phụ thuộc ORM. Hỗ trợ:
 * <ul>
 *   <li>Kiểm tra số row ({@code expectedRows})</li>
 *   <li>So sánh data từng row — partial match (chỉ assert các field khai báo)</li>
 *   <li>Expected data inline trong YAML hoặc load từ file JSON</li>
 *   <li>Ignore fields (id, timestamp...)</li>
 *   <li>Placeholder: {@code $exists}, {@code $notNull}, {@code regex:...}, {@code contains:...}</li>
 * </ul>
 */
public class YamlDbVerifier {

    private static final Logger log = LoggerFactory.getLogger(YamlDbVerifier.class);

    private final DataSource dataSource;
    private final JsonMapper jsonMapper;

    public YamlDbVerifier(DataSource dataSource, JsonMapper jsonMapper) {
        this.dataSource = dataSource;
        this.jsonMapper = jsonMapper;
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Verify danh sách spec — gom tất cả lỗi rồi throw một lần.
     *
     * @param specs     danh sách DbVerifySpec từ YAML
     * @param testName  tên test case (dùng trong thông báo lỗi)
     * @param baseClasspath base classpath thư mục YAML (để resolve expectedFile tương đối)
     */
    public void verify(List<DbVerifySpec> specs, String testName, String baseClasspath) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        List<String> errors = new ArrayList<>();
        for (DbVerifySpec spec : specs) {
            try {
                verifySingle(spec, testName, baseClasspath);
            } catch (Exception e) {
                errors.add("[" + spec.getTable() + "] " + e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new AssertionError("DB verification failed for '" + testName + "':\n  - "
                    + String.join("\n  - ", errors));
        }
    }

    // ── Single spec ───────────────────────────────────────────────────────

    private void verifySingle(DbVerifySpec spec, String testName, String baseClasspath) throws Exception {
        if (spec.getTable() == null || spec.getTable().isBlank()) {
            throw new IllegalArgumentException("dbVerify[].table is required");
        }

        // Validate: không được khai báo cả expected lẫn expectedFile
        if (!spec.getExpected().isEmpty() && spec.getExpectedFile() != null) {
            throw new IllegalArgumentException(
                    "Test '" + testName + "', table '" + spec.getTable()
                    + "': define either 'expected' or 'expectedFile', not both");
        }

        // Load expected data (ưu tiên file)
        List<Map<String, Object>> expectedRows = resolveExpected(spec, testName, baseClasspath);

        // Query DB
        List<Map<String, Object>> actualRows = queryDb(spec);

        log.debug("DB verify '{}' table='{}' where={} actual_rows={}",
                testName, spec.getTable(), spec.getWhere(), actualRows.size());

        // Assert số row
        if (spec.getExpectedRows() >= 0) {
            if (actualRows.size() != spec.getExpectedRows()) {
                throw new AssertionError("Expected " + spec.getExpectedRows() + " row(s) but found "
                        + actualRows.size() + " in table '" + spec.getTable()
                        + "' with where=" + spec.getWhere());
            }
        }

        // Assert data
        if (!expectedRows.isEmpty()) {
            if (expectedRows.size() > actualRows.size()) {
                throw new AssertionError("Expected " + expectedRows.size() + " row(s) to verify but only "
                        + actualRows.size() + " row(s) found in table '" + spec.getTable() + "'");
            }
            Set<String> ignoreSet = spec.getIgnoreFields().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            for (int i = 0; i < expectedRows.size(); i++) {
                Map<String, Object> expected = expectedRows.get(i);
                Map<String, Object> actual = actualRows.get(i);
                assertRow(expected, actual, ignoreSet, spec.getTable(), i);
            }
        }
    }

    // ── DB query ──────────────────────────────────────────────────────────

    private List<Map<String, Object>> queryDb(DbVerifySpec spec) throws Exception {
        String sql = buildSql(spec);
        List<Object> params = new ArrayList<>(spec.getWhere().values());

        log.debug("DB query: {} params={}", sql, params);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return mapResultSet(rs);
            }
        }
    }

    private String buildSql(DbVerifySpec spec) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ")
                .append(spec.getTable());

        if (!spec.getWhere().isEmpty()) {
            sql.append(" WHERE ");
            sql.append(spec.getWhere().keySet().stream()
                    .map(col -> col + " = ?")
                    .collect(Collectors.joining(" AND ")));
        }

        if (!spec.getOrderBy().isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", spec.getOrderBy()));
        }

        return sql.toString();
    }

    private List<Map<String, Object>> mapResultSet(ResultSet rs) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                // Dùng lowercase để so sánh case-insensitive với expected
                row.put(meta.getColumnName(i).toLowerCase(), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    // ── Row assertion ─────────────────────────────────────────────────────

    private void assertRow(Map<String, Object> expected, Map<String, Object> actual,
                           Set<String> ignoreSet, String table, int rowIndex) throws Exception {
        List<String> fieldErrors = new ArrayList<>();

        for (Map.Entry<String, Object> entry : expected.entrySet()) {
            String col = entry.getKey().toLowerCase();
            Object expectedVal = entry.getValue();

            if (ignoreSet.contains(col)) {
                continue;
            }

            if (!actual.containsKey(col)) {
                fieldErrors.add("column '" + col + "' not found in result set");
                continue;
            }

            Object actualVal = actual.get(col);

            try {
                assertValue(col, expectedVal, actualVal);
            } catch (AssertionError e) {
                fieldErrors.add(e.getMessage());
            }
        }

        if (!fieldErrors.isEmpty()) {
            throw new Exception("Row[" + rowIndex + "] in table '" + table + "' mismatches:\n    "
                    + String.join("\n    ", fieldErrors));
        }
    }

    private void assertValue(String col, Object expected, Object actual) {
        // Null expected → chỉ kiểm tra column tồn tại (đã kiểm tra trên)
        if (expected == null) {
            return;
        }

        if (expected instanceof String str) {
            switch (str) {
                case "$exists" -> {
                    if (actual == null) {
                        throw new AssertionError("column '" + col + "' expected to exist but was null");
                    }
                }
                case "$notNull" -> {
                    if (actual == null || actual.toString().isBlank()) {
                        throw new AssertionError("column '" + col + "' expected non-null/non-blank but was: " + actual);
                    }
                }
                case "$null" -> {
                    if (actual != null) {
                        throw new AssertionError("column '" + col + "' expected null but was: " + actual);
                    }
                }
                default -> {
                    if (str.startsWith("regex:")) {
                        String pattern = str.substring("regex:".length());
                        String actualStr = actual != null ? actual.toString() : "";
                        if (!Pattern.compile(pattern).matcher(actualStr).matches()) {
                            throw new AssertionError("column '" + col + "' expected regex '" + pattern
                                    + "' but was: " + actualStr);
                        }
                    } else if (str.startsWith("contains:")) {
                        String fragment = str.substring("contains:".length());
                        String actualStr = actual != null ? actual.toString() : "";
                        if (!actualStr.contains(fragment)) {
                            throw new AssertionError("column '" + col + "' expected to contain '"
                                    + fragment + "' but was: " + actualStr);
                        }
                    } else if (str.startsWith("gt:")) {
                        double threshold = Double.parseDouble(str.substring("gt:".length()).trim());
                        double actualNum = Double.parseDouble(actual != null ? actual.toString() : "0");
                        if (actualNum <= threshold) {
                            throw new AssertionError("column '" + col + "' expected > " + threshold
                                    + " but was: " + actualNum);
                        }
                    } else if (str.startsWith("lt:")) {
                        double threshold = Double.parseDouble(str.substring("lt:".length()).trim());
                        double actualNum = Double.parseDouble(actual != null ? actual.toString() : "0");
                        if (actualNum >= threshold) {
                            throw new AssertionError("column '" + col + "' expected < " + threshold
                                    + " but was: " + actualNum);
                        }
                    } else {
                        // So sánh string → so sánh toString()
                        String actualStr = actual != null ? actual.toString() : null;
                        if (!str.equals(actualStr)) {
                            throw new AssertionError("column '" + col + "' expected '" + str
                                    + "' but was '" + actualStr + "'");
                        }
                    }
                }
            }
        } else {
            // Number, Boolean, etc. — convert sang string để so sánh cross-type safe
            // (JDBC có thể trả BigDecimal, Integer, Long... tùy driver)
            String expectedStr = expected.toString();
            String actualStr = actual != null ? actual.toString() : null;

            // Numeric: so sánh theo BigDecimal để tránh "1500000.0" != "1500000.00"
            try {
                java.math.BigDecimal expectedNum = new java.math.BigDecimal(expectedStr);
                java.math.BigDecimal actualNum = actualStr != null
                        ? new java.math.BigDecimal(actualStr) : null;
                if (actualNum == null || expectedNum.compareTo(actualNum) != 0) {
                    throw new AssertionError("column '" + col + "' expected " + expectedStr
                            + " but was " + actualStr);
                }
            } catch (NumberFormatException e) {
                // Không phải number → so sánh string thuần
                if (!expectedStr.equals(actualStr)) {
                    throw new AssertionError("column '" + col + "' expected '" + expectedStr
                            + "' but was '" + actualStr + "'");
                }
            }
        }
    }

    // ── Expected data resolution ──────────────────────────────────────────

    private List<Map<String, Object>> resolveExpected(DbVerifySpec spec, String testName,
                                                      String baseClasspath) {
        if (spec.getExpectedFile() != null) {
            return loadJsonFile(spec.getExpectedFile(), baseClasspath, testName);
        }
        return spec.getExpected();
    }

    private List<Map<String, Object>> loadJsonFile(String filePath, String baseClasspath,
                                                    String testName) {
        String resolved = resolveClasspath(filePath, baseClasspath);
        InputStream in = YamlDbVerifier.class.getResourceAsStream(resolved);
        if (in == null && !filePath.startsWith("/")) {
            in = YamlDbVerifier.class.getResourceAsStream("/" + filePath);
        }
        if (in == null) {
            throw new IllegalArgumentException(
                    "DB expectedFile not found on classpath: '" + filePath
                    + "' for test '" + testName + "'");
        }
        final InputStream finalIn = in;
        try (finalIn) {
            return jsonMapper.readValue(finalIn, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read DB expectedFile: '" + filePath + "'", e);
        }
    }

    private String resolveClasspath(String filePath, String baseClasspath) {
        if (filePath.startsWith("/")) {
            return filePath;
        }
        if (baseClasspath != null && !baseClasspath.isBlank()) {
            int lastSlash = baseClasspath.lastIndexOf('/');
            String dir = lastSlash >= 0 ? baseClasspath.substring(0, lastSlash + 1) : "/";
            return dir + filePath;
        }
        return "/" + filePath;
    }
}
