# Co so du lieu HotelNT (SQL Server)

> Legacy: `schema.sql` va `seed.sql` duoc giu lai de doi chieu trong qua trinh
> chuyen doi, nhung khong con la nguon khoi tao database. Ung dung dung Liquibase
> tai `src/main/resources/db/changelog`.

## 1. Bat SQL Server
```bash
docker compose up -d          # tu thu muc goc du an
```

## 2. Chay Liquibase qua Spring Boot
Dat `DB_URL`, `DB_USER` va `DB_PASSWORD`, sau do chay:
```bash
./mvnw spring-boot:run
```
Schema duoc kiem tra bang `ddl-auto: validate`. Khong chay `schema.sql`/`seed.sql`
thu cong va khong xoa database Azure hien huu.

Du lieu demo chi duoc nap khi bat context:
```bash
SPRING_LIQUIBASE_CONTEXTS=dev ./mvnw spring-boot:run
```

## 3. Bat ket noi trong Spring Boot
Trong `src/main/resources/application.yml`: xoa 2 dong `autoconfigure.exclude` va bo comment:
```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=HotelNT;encrypt=true;trustServerCertificate=true
    username: sa
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate    # Liquibase quan ly schema, Hibernate chi kiem tra mapping
```

## 4. Tai khoan mau
| Vai tro | Dang nhap | Mat khau | Ghi chu |
|---|---|---|---|
| Admin | admin@hotelnt.local | Admin@123 | |
| Host | host1@hotelnt.local | Host@1234 | co 3 co so (APPROVED, PENDING, REJECTED) |
| Host | host2@hotelnt.local | Host@1234 | co 2 APPROVED, 1 DRAFT |
| Host | host3@hotelnt.local | Host@1234 | bi KHOA, co 1 co so APPROVED (khach khong duoc thay) |

## 5. Quy uoc gia tri (khop API_DESIGN.xlsx)
- `CoSoLuuTru.trangThai`: DRAFT, PENDING, APPROVED, REJECTED, HIDDEN, REMOVED
- `TaiKhoan.vaiTro`: HOST, ADMIN; `TaiKhoan.trangThai`: ACTIVE, LOCKED
- `TienIch.nhom`: TIEN_NGHI, LAN_CAN
- `Media.loai`: IMAGE, VIDEO; `LienHe.loaiLienHe`: SO_DIEN_THOAI, ZALO, FACEBOOK
- `OtpChallenge.loai`: SIGNUP, SIGNIN, FORGOT_PASSWORD
Cac gia tri nay la quy uoc do nhom tu dat (spec chua ghi het); neu doi, them
changeset Liquibase moi, khong sua changeset da phat hanh.

## Legacy dependencies to remove later
- `db/schema.sql`: chi con duoc tham chieu boi tai lieu legacy nay.
- `db/seed.sql`: chi con duoc tham chieu boi tai lieu legacy nay.
- Azure DB da co san can duoc kiem tra bang `DATABASECHANGELOG` sau lan chay Liquibase dau tien.
