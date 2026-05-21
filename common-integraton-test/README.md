# common-integration-test

Thư viện đọc dữ liệu test từ YAML, cấu hình Mockito mock, và chạy HTTP integration test qua MockMvc.

## Dependency

```xml
<dependency>
    <groupId>com.spring.test</groupId>
    <artifactId>common-integraton-test</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

## Định dạng YAML

```yaml
suite: my-api

# Mock toàn suite (bean phải là @MockitoBean / Mockito mock)
mocks:
  - bean: myRepository
    stubs:
      - method: findById
        args: [1]
        return:
          id: 1
          name: demo

tests:
  - name: multi_step_flow
    description: Nhiều HTTP request tuần tự
    steps:
      - request:
          method: POST
          path: /api/items
          body: { name: first }
        expected:
          status: 201
        capture:
          id: $.id
      - request:
          method: GET
          path: /api/items/${id}
        expected:
          status: 200

  - name: create_item
    description: Mô tả test
    enabled: true
    request:
      method: POST
      path: /api/items
      headers:
        Content-Type: application/json
      queryParams:
        page: "0"
      body:
        name: demo
    expected:
      status: 201
      headers:
        Location: "regex:.*/api/items/\\d+"
      jsonPath:
        "$.id": "$exists"
        "$.name": demo
        "$.detail": "contains:error"
      body:          # optional — so khớp toàn bộ JSON
        name: demo
```

### Giá trị đặc biệt trong `jsonPath`

| Giá trị | Ý nghĩa |
|--------|---------|
| `$exists` | Path tồn tại |
| `$notNull` | Không rỗng |
| `regex:...` | Khớp regex |
| `contains:...` | Chuỗi chứa đoạn con |

### Mock stub

| Field | Mô tả |
|-------|--------|
| `bean` | Tên Spring bean |
| `type` | FQCN (nếu không có bean name) |
| `stubs[].method` | Tên method |
| `stubs[].args` | Danh sách tham số |
| `stubs[].return` / `returnValue` | Giá trị trả về |
| `stubs[].throws` | `ExceptionClass: message` |

## Cách dùng

### 1. ParameterizedTest — mỗi case một dòng trên report (khuyến nghị)

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@YamlTest("yaml/my-tests.yml")
class MyYamlTest extends AbstractIntegrationTest implements YamlTestContext, YamlIntegrationTestSupport {

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;
    @Autowired ApplicationContext applicationContext;

    @ParameterizedTest(name = "[{index}] {0}")
    @YamlTestSource("yaml/my-tests.yml")
    void runYamlCase(String testName, String description) {
        runYamlTest(testName);
    }
}
```

Report Surefire/JUnit sẽ hiển thị từng case, ví dụ:
`[1] create_transfer_success`, `[2] create_duplicate_code`, …

### 2. DynamicTest (nhóm dưới một test factory)

```java
@TestFactory
Stream<DynamicTest> runFromYaml() {
    return YamlDynamicTestFactory.stream(this, "yaml/my-tests.yml");
}
```

### 3. Chạy cả file một lần

```java
@Test
void runAll() {
    runYamlTests("yaml/my-tests.yml");
}
```

### 4. Chạy một case

```java
@Test
void runOne() {
    runYamlTest("yaml/my-tests.yml", "create_item");
}
```

### 5. Đọc YAML thủ công

```java
TestSuite suite = new YamlTestDataReader().loadClasspath("yaml/my-tests.yml");
```

## Lưu ý

- Mock chỉ áp dụng được khi bean là **Mockito mock** (`@MockitoBean`).
- Test dùng **MockMvc** — phù hợp `@WebMvcTest` / `@SpringBootTest`.
- Các test trong cùng file YAML **độc lập** trừ khi bạn tự quản lý dữ liệu (`@BeforeEach`, thứ tự `enabled`).
