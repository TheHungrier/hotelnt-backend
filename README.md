# HotelNT Backend (Java 17 + Spring Boot + SQL Server)

## Chay
```bash
./mvnw spring-boot:run          # http://localhost:3000/api/health
./mvnw test
```
- Swagger UI: http://localhost:3000/api/swagger-ui.html
- Hien tai cac endpoint public tra DU LIEU MAU (`MockData.java`) dung hinh dang JSON trong `docs/API_DESIGN.xlsx`.
- Chua can SQL Server de chay. Khi san sang DB, xem ghi chu dau `application.yml`.

### Bien moi truong
Tao file bien moi truong local tu file mau:

```powershell
Copy-Item .env.example .env
```

Hoac tren macOS/Linux:

```bash
cp .env.example .env
```

Cap nhat cac gia tri `change_me` va cac bien tuy chon trong `.env`. File `.env`
chi dung cho may local, khong commit file nay va khong dat secret that vao
`.env.example`. Docker Compose tu dong doc file `.env`; khi chay Spring Boot
ngoai Docker, can nap cac bien nay vao shell hoac cau hinh Run/Debug cua IDE.

## Cau truc
```
com.hotelnt
 ├─ common/error   ErrorCode, AppException, GlobalExceptionHandler  (dinh dang loi chung)
 ├─ config         CORS
 └─ modules/<ten>  controller + service + dto (moi nguoi 1 module)
```
Moi nguoi chi sua trong module cua minh; file dung chung (`ErrorCode`, `application.yml`, `pom.xml`) bao truong nhom.

## Database
Liquibase là nguồn quản lý schema và dữ liệu của ứng dụng.

```bash
# Đặt mật khẩu SQL Server cho phiên Docker hiện tại
$env:MSSQL_SA_PASSWORD = "your-local-password"  # PowerShell
docker compose up -d

# Trỏ ứng dụng vào SQL Server bằng biến môi trường
$env:DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=HotelNT;encrypt=true;trustServerCertificate=true"
$env:DB_USER = "sa"
$env:DB_PASSWORD = $env:MSSQL_SA_PASSWORD
./mvnw spring-boot:run
```

Khi khởi động, Liquibase chạy `db/changelog/db.changelog-master.yaml`, tạo
`DATABASECHANGELOG`/`DATABASECHANGELOGLOCK`, rồi áp dụng schema và dữ liệu tham chiếu.
Dữ liệu demo nằm trong context `dev`; bật bằng
`SPRING_LIQUIBASE_CONTEXTS=dev` khi chạy môi trường phát triển.

Thư mục `db/` vẫn được giữ lại để đối chiếu trong đợt chuyển đổi, nhưng không còn
được Docker Compose hoặc ứng dụng tự động chạy. Xem [db/README.md](db/README.md)
để biết các phụ thuộc legacy còn lại.
