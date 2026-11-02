# HotelNT Backend (Java 17 + Spring Boot + SQL Server)

## Chay
```bash
./mvnw spring-boot:run          # http://localhost:3000/api/health
./mvnw test
```
- Swagger UI: http://localhost:3000/api/swagger-ui.html
- Hien tai cac endpoint public tra DU LIEU MAU (`MockData.java`) dung hinh dang JSON trong `docs/API_DESIGN.xlsx`.
- Chua can SQL Server de chay. Khi san sang DB, xem ghi chu dau `application.yml`.

## Cau truc
```
com.hotelnt
 ├─ common/error   ErrorCode, AppException, GlobalExceptionHandler  (dinh dang loi chung)
 ├─ config         CORS
 └─ modules/<ten>  controller + service + dto (moi nguoi 1 module)
```
Moi nguoi chi sua trong module cua minh; file dung chung (`ErrorCode`, `application.yml`, `pom.xml`) bao truong nhom.

## Database
Xem `db/README.md` (docker compose + schema.sql + seed.sql).
