# HotelNT Backend

Hệ thống Backend cho nền tảng đặt phòng và tư vấn cơ sở lưu trú tại Nha Trang.
Xây dựng trên nền tảng Java 17, Spring Boot 3.3.5, SQL Server và tích hợp Chatbot AI.

---

## Mục lục

- [Tổng quan dự án](#tổng-quan-dự-án)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Các phân hệ chức năng](#các-phân-hệ-chức-năng)
- [Cấu trúc mã nguồn](#cấu-trúc-mã-nguồn)
- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Hướng dẫn cài đặt và khởi chạy](#hướng-dẫn-cài-đặt-và-khởi-chạy)
  - [1. Khởi động SQL Server qua Docker Compose](#1-khởi-động-sql-server-qua-docker-compose)
  - [2. Cấu hình biến môi trường (.env)](#2-cấu-hình-biến-môi-trường-env)
  - [3. Chạy ứng dụng Spring Boot](#3-chạy-ứng-dụng-spring-boot)
- [Tài liệu API](#tài-liệu-api)
- [Cấu hình Chatbot AI](#cấu-hình-chatbot-ai)
- [Kiểm thử (Testing)](#kiểm-thử-testing)
- [Lưu ý triển khai](#lưu-ý-triển-khai)

---

## Tổng quan dự án

HotelNT cung cấp hệ thống RESTful API phục vụ ứng dụng web và mobile cho 3 nhóm người dùng:

- Khách hàng (Guest): Tra cứu cơ sở lưu trú, lọc theo ngân sách, khu vực, tiện ích, xem bản đồ tọa độ và tư vấn qua Chatbot AI.
- Chủ cơ sở (Host): Đăng ký tài khoản qua OTP email, tạo và quản lý thông tin cơ sở lưu trú, đăng tải hình ảnh/video, cập nhật bảng giá và gửi duyệt hồ sơ.
- Quản trị viên (Admin): Kiểm duyệt hồ sơ cơ sở lưu trú (phê duyệt / từ chối), quản lý tài khoản host (khóa / kích hoạt) và cấu hình thông tin hệ thống.

---

## Công nghệ sử dụng

- Ngôn ngữ & Framework: Java 17, Spring Boot 3.3.5.
- Cơ sở dữ liệu: Microsoft SQL Server 2022.
- Quản lý phiên bản CSDL: Liquibase (master changelog quản lý schema và migration).
- ORM: Spring Data JPA, Hibernate 6.
- Bảo mật & Xác thực: JJWT (JSON Web Token), BCrypt, xác thực OTP qua SMTP Mail.
- Xử lý hình ảnh: TwelveMonkeys ImageIO (hỗ trợ đọc và kiểm tra định dạng WebP, JPEG, PNG).
- Tài liệu API: SpringDoc OpenAPI 3 (Swagger UI).
- Tích hợp AI: Cloudflare Workers AI (Llama 3.1 8B Instruct) / Google Gemini API (Gemini 3.8 Flash).
- Kiểm thử: JUnit 5, Mockito, AssertJ, Spring MockMvc, Testcontainers.

---

## Các phân hệ chức năng

### 1. Xác thực và Phân quyền (Auth)
- Đăng ký và đăng nhập với cơ chế xác thực OTP 2 bước qua email.
- Cấp token JWT gồm Access Token (thời hạn ngắn) và Refresh Token (lưu trong CSDL).
- Thu hồi phiên đăng nhập (Session Revocation) khi đăng xuất hoặc khi tài khoản bị khóa.
- Luồng quên mật khẩu và đặt lại mật khẩu qua mã OTP xác minh.

### 2. Tra cứu cơ sở lưu trú (Guest & Listing)
- Bộ lọc tiêu chí: Phường, loại hình (Khách sạn, Homestay, Villa, Resort, Căn hộ), tiện ích, khoảng giá.
- Tính toán khoảng cách thực tế qua Google Distance Matrix API.
- Cung cấp API Map Markers nhẹ để hiển thị danh sách vị trí trên bản đồ.
- Cung cấp metadata OpenGraph và thẻ meta cho SEO.

### 3. Quản lý dành cho Host
- Tạo và cập nhật thông tin cơ sở lưu trú.
- Quản lý danh sách hình ảnh và video (giới hạn dung lượng tối đa 100MB).
- Quy trình trạng thái hồ sơ: DRAFT -> PENDING -> APPROVED / REJECTED.

### 4. Quản trị hệ thống (Admin)
- Kiểm duyệt cơ sở lưu trú theo thứ tự hàng đợi FIFO.
- Quản lý tài khoản host (khóa tài khoản vi phạm, mở khóa).
- Cấu hình thông tin liên hệ hệ thống (Hotline, Email hỗ trợ, Logo, Banner).

### 5. Trợ lý Chatbot AI
- Chuẩn hóa tiếng Việt không dấu (normalize): hỗ trợ nhận diện cả câu có dấu và không dấu.
- Lọc cứng dữ liệu: chỉ gợi ý cơ sở lưu trú đã duyệt (APPROVED) và host đang hoạt động (ACTIVE), không vượt ngân sách hoặc sai loại hình.
- Trò chuyện tự nhiên: hỗ trợ giải đáp thân thiện các câu hỏi hàng ngày và điều hướng về nhu cầu lưu trú Nha Trang.
- Tích hợp đa nhà cung cấp: Cloudflare Workers AI, Google Gemini hoặc chế độ fallback nội bộ.

---

## Cấu trúc mã nguồn

```
src/
 ├─ main/
 │   ├─ java/com/hotelnt/
 │   │   ├─ common/error/       # Mã lỗi (ErrorCode) và xử lý ngoại lệ toàn cục
 │   │   ├─ config/             # Cấu hình CORS, Swagger OpenAPI, WebConfig
 │   │   ├─ controller/         # REST Controllers (Auth, Host, Admin, Listing, Chatbot)
 │   │   ├─ domain/             # JPA Entities (CoSoLuuTru, TaiKhoan, Media, TienIch, Phuong...)
 │   │   ├─ dto/                # Request và Response DTOs
 │   │   ├─ helper/             # Các lớp hỗ trợ (ChatbotSpecs, UrlHelper, AiClient...)
 │   │   ├─ repository/         # Spring Data JPA Repositories
 │   │   ├─ service/            # Business Logic Services
 │   │   └─ HotelNTApplication.java
 │   └─ resources/
 │       ├─ application.yml     # Cấu hình ứng dụng
 │       └─ db/changelog/       # Liquibase changelogs
 └─ test/                       # Unit tests và Integration tests
```

---

## Yêu cầu hệ thống

- JDK 17 trở lên.
- Docker & Docker Compose (để chạy SQL Server local).
- Maven Wrapper được tích hợp sẵn trong dự án.

---

## Hướng dẫn cài đặt và khởi chạy

### 1. Khởi động SQL Server qua Docker Compose

Khởi động container SQL Server 2022 ở cổng 1433:

```powershell
# Windows (PowerShell):
$env:MSSQL_SA_PASSWORD = "YourStrong@Password123"
docker compose up -d
```

```bash
# Linux / macOS:
export MSSQL_SA_PASSWORD="YourStrong@Password123"
docker compose up -d
```

### 2. Cấu hình biến môi trường (.env)

Tạo file `.env` từ file mẫu `.env.example`:

```powershell
# Windows (PowerShell):
Copy-Item .env.example .env
```

```bash
# Linux / macOS:
cp .env.example .env
```

Thiết lập các thông số cần thiết trong `.env`:

```ini
PORT=3000

DB_URL=jdbc:sqlserver://localhost:1433;databaseName=HotelNT;encrypt=true;trustServerCertificate=true
DB_USER=sa
DB_PASSWORD=YourStrong@Password123
MSSQL_SA_PASSWORD=YourStrong@Password123

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=your-email@gmail.com

APP_AUTH_JWT_SECRET=your-super-secret-jwt-key-at-least-32-chars-long
APP_PUBLIC_BASE_URL=http://localhost:3000/api

AI_PROVIDER=none
```

### 3. Chạy ứng dụng Spring Boot

Khởi động server backend:

```powershell
# Windows:
.\mvnw.cmd spring-boot:run
```

```bash
# Linux / macOS:
./mvnw spring-boot:run
```

Kiểm tra trạng thái ứng dụng:
- Health check: http://localhost:3000/api/health
- Kết quả: `{"status":"UP"}`

---

## Tài liệu API

Ứng dụng tích hợp Swagger UI để tra cứu và kiểm thử trực tiếp các endpoint:

- Swagger UI: http://localhost:3000/api/swagger-ui.html
- OpenAPI Spec: http://localhost:3000/api/v3/api-docs

Tất cả các endpoint có context path mặc định là `/api`.

---

## Cấu hình Chatbot AI

Chọn nhà cung cấp AI thông qua biến `AI_PROVIDER`:

### 1. Cloudflare Workers AI
```ini
AI_PROVIDER=cloudflare
CF_ACCOUNT_ID=your_account_id
CF_API_TOKEN=your_api_token
CF_AI_MODEL=@cf/meta/llama-3.1-8b-instruct
```

### 2. Google Gemini
```ini
AI_PROVIDER=gemini
GEMINI_API_KEY=your_gemini_api_key
GEMINI_MODEL=gemini-3.8-flash
# GEMINI_BASE_URL=https://generativelanguage.googleapis.com
```

### 3. Không dùng AI ngoài (mặc định)
```ini
AI_PROVIDER=none
```
Chatbot sử dụng bộ phân tích NLP nội bộ và trả về câu trả lời mẫu, không phụ thuộc vào API bên ngoài.

---

## Kiểm thử (Testing)

Chạy toàn bộ test suite:

```powershell
.\mvnw.cmd test
```

Chạy riêng bộ kiểm thử Chatbot và Helper:

```powershell
.\mvnw.cmd test "-Dtest=Chatbot*Test,UrlHelperTest"
```

---

## Lưu ý triển khai

- Biến môi trường: Trên môi trường Cloud (Azure App Service, AWS, VPS), cấu hình đầy đủ các biến trong `.env` vào Application Settings của hệ thống host.
- Đường dẫn ảnh tuyệt đối: Thiết lập `APP_PUBLIC_BASE_URL=https://<domain>/api` để hệ thống trả về đường dẫn ảnh đầy đủ cho Frontend.
- Kết nối CSDL: Dùng chuỗi kết nối an toàn với `encrypt=true;trustServerCertificate=false` khi kết nối tới CSDL trên Cloud.
