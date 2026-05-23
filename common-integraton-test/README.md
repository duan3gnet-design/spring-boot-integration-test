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

---

## WireMock

Dùng để stub HTTP call ra ngoài (external service) thay vì mock Spring bean.

### Dependency

```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock</artifactId>
    <version>3.13.0</version>
    <scope>test</scope>
</dependency>
```

### Định dạng YAML

```yaml
suite: payment-api

# WireMock stub cấp suite — áp dụng cho tất cả test case
wireMocks:
  - server: payment-service        # tên server đăng ký trong WireMockServerRegistry
    stubs:
      - method: POST
        url: /api/payments          # khớp chính xác
        status: 200
        responseBody:               # inline
          paymentId: PAY-001
          status: SUCCESS
        responseHeaders:
          Content-Type: application/json

tests:
  - name: call_external_with_delay
    wireMocks:
      - server: payment-service    # WireMock stub cấp test case
        stubs:
          - method: GET
            urlPattern: /api/payments/.*   # regex
            status: 200
            responseBodyFile: wiremock/payment-detail.json   # load từ classpath
            delayMs: 300
    request:
      method: POST
      path: /api/orders
    expected:
      status: 201

  - name: external_returns_500
    wireMocks:
      - server: payment-service
        stubs:
          - method: POST
            urlPath: /api/payments   # path only, bỏ qua query string
            requestBody:
              contains: "amount"     # khớp body chứa chuỗi
            requestHeaders:
              X-Api-Key: secret
            status: 500
    request:
      method: POST
      path: /api/orders
    expected:
      status: 503
```

### URL matching

| Field | Ý nghĩa |
|-------|---------|
| `url` | Khớp chính xác (bao gồm query string) |
| `urlPattern` | Regex |
| `urlPath` | Chính xác path, bỏ qua query string |

### Request body matching (`requestBody`)

| Key | Ý nghĩa |
|-----|---------|
| `contains` | Body chứa chuỗi |
| `matches` | Body khớp regex |
| `equalTo` | Body bằng chính xác |

### Cách dùng trong test class

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@YamlTest("yaml/payment-tests.yml")
@EnableWireMock({
    @WireMockServer(name = "payment-service"),      // dynamic port
    @WireMockServer(name = "notification-service")  // dynamic port
})
class PaymentYamlTest extends AbstractIntegrationTest
        implements YamlTestContext, YamlIntegrationTestSupport, WireMockSupport {

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;
    @Autowired ApplicationContext applicationContext;

    // Trỏ Spring property về WireMock port — gọi getWireMockRegistry() từ WireMockSupport
    @DynamicPropertySource
    static void wireMockProperties(DynamicPropertyRegistry registry) {
        WireMockServerRegistry wm = WireMockContextHolder.get(); // static context
        registry.add("payment.base-url",
            () -> "http://localhost:" + wm.get("payment-service").port());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @YamlTestSource("yaml/payment-tests.yml")
    void runYamlCase(String testName, String description) {
        runYamlTest(testName);
    }
}
```

Nếu chỉ có một server, bỏ qua `name`:

```
@EnableWireMock(@WireMockServer)  // dùng làm default server
```

> **Lifecycle tự động:**
> - `BeforeAll` — start tất cả server
> - `BeforeEach` — reset tất cả mapping (stub sạch trước mỗi test)
> - `AfterAll` — stop tất cả server
