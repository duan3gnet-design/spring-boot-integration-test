# Transaction API (Spring Boot 4.0.5 + PostgreSQL)

API quản lý dữ liệu giao dịch trong module `test-api`.

## Yêu cầu

- Java 21
- PostgreSQL
- Maven

## Database

Tạo database:

```sql
CREATE DATABASE transaction_db;
```

Chạy migration:

```bash
cd migration
mvn spring-boot:run
```

Biến môi trường (tùy chọn):

- `TRANSACTION_DB_URL` — mặc định `jdbc:postgresql://localhost:5432/transaction_db`
- `DB_USERNAME` — mặc định `postgres`
- `DB_PASSWORD` — mặc định `postgres`

## Chạy API

```bash
cd test-api
mvn spring-boot:run
```

API lắng nghe tại `http://localhost:8090`.

## Endpoints

| Method | Path | Mô tả |
|--------|------|--------|
| GET | `/api/transactions` | Danh sách có phân trang + lọc |
| GET | `/api/transactions/{id}` | Chi tiết giao dịch |
| POST | `/api/transactions` | Tạo giao dịch (status PENDING) |
| PUT | `/api/transactions/{id}` | Cập nhật (chỉ PENDING) |
| PATCH | `/api/transactions/{id}/status` | Đổi trạng thái |
| DELETE | `/api/transactions/{id}` | Xóa (chỉ PENDING) |

### Query params (GET list)

- `status` — PENDING, COMPLETED, FAILED, CANCELLED
- `type` — DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT
- `fromAccount`, `toAccount`
- `fromDate`, `toDate` — ISO-8601 (ví dụ `2026-05-01T00:00:00Z`)
- `page`, `size`, `sort` — Spring Data pagination

### Ví dụ tạo giao dịch chuyển khoản

POST /api/transactions
```json
{
  "transactionCode": "TXN-20260521-001",
  "fromAccount": "ACC-001",
  "toAccount": "ACC-002",
  "amount": 1500000.00,
  "currency": "VND",
  "type": "TRANSFER",
  "description": "Chuyen tien noi bo"
}
```

### Ví dụ cập nhật trạng thái

PATCH /api/transactions/1/status
```json
{
  "status": "COMPLETED"
}
```

## Integration tests

Yêu cầu **Docker Desktop** đang chạy (Testcontainers PostgreSQL).

```bash
cd test-api
mvn test
```

Nếu Docker không chạy, các test integration sẽ bị **SKIP** (không fail build).

## YAML-driven tests (`common-integraton-test`)

Thư viện đọc test case từ file YAML, cấu hình mock và chạy qua MockMvc.

```bash
# File test: test-api/src/test/resources/yaml/transaction-tests.yml
mvn -pl test-api test -Dtest=TransactionIntegrationTest
mvn -pl test-api test -Dtest=TransactionMockYamlIntegrationTest
```

Chi tiết: [common-integraton-test/README.md](common-integraton-test/README.md)
