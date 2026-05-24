package com.spring.test.yaml.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Khai báo DB assertion sau khi chạy HTTP request.
 *
 * <p>Ví dụ YAML:
 * <pre>
 * dbVerify:
 *   - table: transactions
 *     where:
 *       transaction_code: TXN-001
 *     expectedRows: 1
 *     expected:
 *       - status: PENDING
 *         amount: 1500000.00
 *         from_account: ACC-001
 *     ignoreFields: [id, created_at, updated_at]
 *
 *   # Dùng file JSON thay cho inline expected
 *   - table: accounts
 *     where:
 *       account_number: ACC-001
 *     expectedFile: db/account-after-transfer.json
 *     ignoreFields: [id, updated_at]
 *
 *   # Chỉ verify số row, không check data
 *   - table: audit_logs
 *     where:
 *       action: TRANSFER
 *     expectedRows: 1
 * </pre>
 *
 * <p>Trong {@code expected} (inline hoặc file JSON), mỗi row là một object.
 * Hỗ trợ placeholder giống HTTP body assert:
 * <ul>
 *   <li>{@code "$exists"} — field tồn tại và không null</li>
 *   <li>{@code "$notNull"} — tồn tại, không null, không blank</li>
 *   <li>{@code "regex:..."} — khớp regex</li>
 *   <li>{@code "contains:..."} — chứa chuỗi</li>
 *   <li>{@code null} — chỉ kiểm tra field tồn tại</li>
 * </ul>
 */
public class DbVerifySpec {

    /** Tên bảng cần query. */
    private String table;

    /**
     * Điều kiện WHERE — key: tên cột, value: giá trị cần khớp chính xác.
     * Các điều kiện được nối bằng AND.
     */
    private Map<String, Object> where = new LinkedHashMap<>();

    /**
     * Số row kỳ vọng sau WHERE.
     * {@code -1} = không kiểm tra số row (chỉ kiểm tra data).
     */
    private int expectedRows = -1;

    /**
     * Expected data — inline trong YAML.
     * Mỗi phần tử là một row (map column → value).
     * So sánh theo thứ tự với kết quả query.
     */
    private List<Map<String, Object>> expected = new ArrayList<>();

    /**
     * Đường dẫn file JSON chứa expected data (classpath hoặc relative với file YAML).
     * Nội dung file là JSON array of objects, ví dụ:
     * <pre>
     * [
     *   { "status": "PENDING", "amount": 1500000.00 }
     * ]
     * </pre>
     * Nếu khai báo cả {@code expected} lẫn {@code expectedFile} → throw lỗi.
     */
    private String expectedFile;

    /**
     * Danh sách cột bỏ qua khi so sánh (ví dụ: id, created_at, updated_at).
     * Vẫn được fetch từ DB nhưng không assert.
     */
    private List<String> ignoreFields = new ArrayList<>();

    /**
     * Thứ tự sort kết quả query — tránh flaky test khi DB trả về nhiều row.
     * Ví dụ: {@code [amount, created_at DESC]}
     */
    private List<String> orderBy = new ArrayList<>();

    // ── Getters / Setters ─────────────────────────────────────────────────

    public String getTable() { return table; }
    public void setTable(String table) { this.table = table; }

    public Map<String, Object> getWhere() { return where; }
    public void setWhere(Map<String, Object> where) {
        this.where = where != null ? where : new LinkedHashMap<>();
    }

    public int getExpectedRows() { return expectedRows; }
    public void setExpectedRows(int expectedRows) { this.expectedRows = expectedRows; }

    public List<Map<String, Object>> getExpected() { return expected; }
    public void setExpected(List<Map<String, Object>> expected) {
        this.expected = expected != null ? expected : new ArrayList<>();
    }

    public String getExpectedFile() { return expectedFile; }
    public void setExpectedFile(String expectedFile) { this.expectedFile = expectedFile; }

    public List<String> getIgnoreFields() { return ignoreFields; }
    public void setIgnoreFields(List<String> ignoreFields) {
        this.ignoreFields = ignoreFields != null ? ignoreFields : new ArrayList<>();
    }

    public List<String> getOrderBy() { return orderBy; }
    public void setOrderBy(List<String> orderBy) {
        this.orderBy = orderBy != null ? orderBy : new ArrayList<>();
    }

    public boolean hasExpectedData() {
        return !expected.isEmpty() || expectedFile != null;
    }
}
