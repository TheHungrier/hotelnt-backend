# Co so du lieu HotelNT (SQL Server)

## 1. Bat SQL Server
```bash
docker compose up -d          # tu thu muc goc du an
```

## 2. Tao bang va nap du lieu mau
```bash
docker exec -it hotelnt-sqlserver /opt/mssql-tools18/bin/sqlcmd -I -C -S localhost -U sa -P "HotelNT@2026" -i /db/schema.sql
docker exec -it hotelnt-sqlserver /opt/mssql-tools18/bin/sqlcmd -I -C -S localhost -U sa -P "HotelNT@2026" -i /db/seed.sql
```
(Neu khong dung Docker: mo schema.sql roi seed.sql bang SSMS / Azure Data Studio va bam Execute.)
Co `-I` trong lenh de sqlcmd bat QUOTED_IDENTIFIER (neu thieu se loi khi insert vi co chi muc loc).
Chay lai schema.sql se XOA du lieu cu.

## 3. Bat ket noi trong Spring Boot
Trong `src/main/resources/application.yml`: xoa 2 dong `autoconfigure.exclude` va bo comment:
```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=HotelNT;encrypt=true;trustServerCertificate=true
    username: sa
    password: ${DB_PASSWORD:HotelNT@2026}
  jpa:
    hibernate:
      ddl-auto: none        # bang do script SQL quan ly, KHONG de Hibernate tu tao/sua
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
Cac gia tri nay la quy uoc do nhom tu dat (spec chua ghi het); neu doi, sua ca CHECK trong schema.sql.
Chi truong nhom sua schema.sql. Can them cot thi nhan truong nhom.
