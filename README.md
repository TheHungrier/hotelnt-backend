# 🏨 HotelNT Backend

> **Hệ thống Backend Nền tảng Đặt phòng & Tư vấn Lưu trú Du lịch Nha Trang**  
> Xây dựng trên nền tảng **Java 17**, **Spring Boot 3.3.5**, **SQL Server** và tích hợp **Trí tuệ nhân tạo (Chatbot AI)**.

---

## 📌 Mục lục

- [Tổng quan dự án](#-tổng-quan-dự-án)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Các tính năng chính](#-các-tính-năng-chính)
- [Cấu trúc mã nguồn](#-cấu-trúc-mã-nguồn)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Hướng dẫn cài đặt & Chạy ứng dụng](#-hướng-dẫn-cài-đặt--chạy-ứng-dụng)
  - [1. Khởi động SQL Server bằng Docker Compose](#1-khởi-động-sql-server-bằng-docker-compose)
  - [2. Cấu hình biến môi trường (.env)](#2-cấu-hình-biến-môi-trường-env)
  - [3. Chạy ứng dụng Spring Boot](#3-chạy-ứng-dụng-spring-boot)
- [Tài liệu API (Swagger / OpenAPI)](#-tài-liệu-api-swagger--openapi)
- [Cấu hình Chatbot AI](#-cấu-hình-chatbot-ai)
- [Chạy kiểm thử (Testing)](#-chạy-kiểm-thử-testing)
- [Triển khai sản xuất (Production / Azure)](#-triển-khai-sản-xuất-production--azure)

---

## 🌟 Tổng quan dự án

**HotelNT** là nền tảng số hỗ trợ khách du lịch tra cứu, khám phá và kết nối trực tiếp với các cơ sở lưu trú (Khách sạn, Homestay, Villa, Resort, Căn hộ) tại thành phố Nha Trang. 

Hệ thống Backend cung cấp toàn bộ RESTful API cho ứng dụng Web và Mobile:
- **Khách hàng (Guest):** Tra cứu, lọc theo tiêu chí ngân sách/vị trí, xem bản đồ trực quan, trò chuyện với trợ lý AI.
- **Chủ cơ sở (Host):** Đăng ký tài khoản qua OTP email, quản lý thông tin cơ sở lưu trú, tải ảnh/video, cập nhật bảng giá và gửi duyệt.
- **Quản trị viên (Admin):** Kiểm duyệt cơ sở lưu trú (phê duyệt / từ chối), quản lý tài khoản Host (khóa / kích hoạt), theo dõi vi phạm và cấu hình toàn hệ thống.

---

## 🛠 Công nghệ sử dụng

| Phân hệ / Thư viện | Công nghệ | Mục đích |
|---|---|---|
| **Core Framework** | Spring Boot 3.3.5 | Nền tảng ứng dụng Java enterprise hiện đại |
| **Language** | Java 17 (LTS) | Hỗ trợ Record, Pattern Matching, Text Blocks |
| **Database** | Microsoft SQL Server 2022 | Lưu trữ dữ liệu quan hệ chính |
| **ORM / Data Access** | Spring Data JPA / Hibernate 6 | Thao tác dữ liệu, Specification filter, Pessimistic Locking |
| **Database Migration** | Liquibase | Quản lý phiên bản schema và dữ liệu tham chiếu (master changelog) |
| **Authentication & Token** | JJWT (0.12.6) + BCrypt | Mã hóa mật khẩu, xác thực JWT Access Token & Refresh Token |
| **Email Service** | Spring Boot Starter Mail | Gửi mã xác thực OTP qua giao thức SMTP |
| **Image Processing** | TwelveMonkeys ImageIO (WebP) | Hỗ trợ đọc, validate và xử lý định dạng ảnh hiện đại WebP |
| **API Documentation** | SpringDoc OpenAPI 3 (Swagger UI) | Tự động sinh tài liệu và thử nghiệm API tương tác |
| **AI Providers** | Cloudflare Workers AI / Google Gemini | Trợ lý tư vấn AI tự nhiên, xử lý ngôn ngữ tiếng Việt |
| **Testing** | JUnit 5, Mockito, AssertJ, Testcontainers | Kiểm thử đơn vị (Unit Test) và kiểm thử tích hợp (Integration Test) |

---

## 🚀 Các tính năng chính

### 1. Phân hệ Xác thực & Bảo mật (Auth Module)
- **Đăng ký & Đăng nhập Host/Admin:** Xác thực 2 bước với mã OTP gửi qua Email (thời hạn 5 phút, giới hạn số lần gửi lại).
- **Cơ chế Token:** Cặp `accessToken` (thời hạn ngắn) và `refreshToken` (lưu mã hóa trong DB).
- **Quản lý phiên (Session Revocation):** Thu hồi token khi đăng xuất hoặc khi tài khoản bị khóa.
- **Quên mật khẩu:** Luồng khôi phục an toàn qua mã OTP xác minh email.

### 2. Phân hệ Khám phá & Tra cứu (Guest & Listing Module)
- **Bộ lọc đa tiêu chí:** Lọc theo Phường, Loại hình CSLT, Tiện ích (hồ bơi, wifi, bếp...), khoảng giá tối thiểu/tối đa.
- **Tọa độ & Khoảng cách:** Tìm kiếm theo bán kính hoặc tọa độ thực tế từ Google Distance Matrix API.
- **Bản đồ Map Markers:** API phân cụm tọa độ nhẹ phục vụ hiển thị hàng loạt marker trên bản đồ mà không gây nghẽn băng thông.
- **SEO Metadata:** Cung cấp thông tin OpenGraph, thẻ meta phục vụ chia sẻ mạng xã hội.

### 3. Phân hệ Dành cho Chủ cơ sở (Host Module)
- Đăng tải và chỉnh sửa thông tin CSLT (tên, mô tả, vị trí bản đồ, số điện thoại, Zalo, Facebook).
- Upload và sắp xếp thứ tự đa phương tiện (ảnh JPEG, PNG, WebP tối đa 100MB).
- Quy trình kiểm duyệt minh bạch: `DRAFT` $\rightarrow$ `PENDING` $\rightarrow$ `APPROVED` (hoặc `REJECTED` kèm lý do).

### 4. Phân hệ Quản trị viên (Admin Module)
- Bảng điều khiển kiểm duyệt hồ sơ CSLT theo thứ tự hàng đợi FIFO.
- Quản lý danh sách Host: duyệt tài khoản, khóa tài khoản vi phạm.
- Cấu hình thông tin hệ thống (Hotline, Email hỗ trợ, Logo, Banner).

### 5. Phân hệ Trợ lý ảo AI (Chatbot Module)
- **Xử lý tiếng Việt không dấu (`normalize`):** Người dùng nhắn *"khach san duoi 500k"* hay *"Khách sạn dưới 500k"* đều được nhận diện chính xác.
- **Lọc cứng an toàn:** Tuyệt đối chỉ gợi ý các cơ sở **đã duyệt (`APPROVED`)** và chủ cơ sở đang **hoạt động (`ACTIVE`)**. Không bao giờ gợi ý khách sạn vượt ngân sách hay sai loại hình.
- **Trò chuyện tự nhiên:** Hỗ trợ đối đáp thân thiện, khéo léo dẫn dắt khách tìm phòng ngay cả với những câu hỏi thường ngày (thời tiết, chào hỏi...).
- **Đa nhà cung cấp linh hoạt:** Dễ dàng chuyển đổi giữa Cloudflare Workers AI (`Llama-3.1-8b-instruct`), Google Gemini, hoặc chế độ Fallback không dùng AI.

---

## 📁 Cấu trúc mã nguồn

```
src/
 ├─ main/
 │   ├─ java/com/hotelnt/
 │   │   ├─ common/error/       # Quản lý mã lỗi (ErrorCode), Exception toàn cục (GlobalExceptionHandler)
 │   │   ├─ config/             # Cấu hình CORS, Swagger OpenAPI, Jackson ObjectMapper
 │   │   ├─ controller/         # Lớp Controller tiếp nhận REST Request (Admin, Host, Guest, Chatbot...)
 │   │   ├─ domain/             # Các Entity JPA (CoSoLuuTru, TaiKhoan, Media, TienIch, Phuong...)
 │   │   ├─ dto/                # Data Transfer Objects (Request/Response theo chuẩn API Design)
 │   │   ├─ helper/             # Các tiện ích (ChatbotSpecs, UrlHelper, AiClient, Gemini/Cloudflare Client)
 │   │   ├─ repository/         # Spring Data JPA Repositories (truy vấn dữ liệu, pessimistic locking)
 │   │   ├─ service/            # Nghiệp vụ lõi (ChatbotService, ListingService, HostService, AuthService...)
 │   │   └─ HotelNTApplication.java
 │   └─ resources/
 │       ├─ application.yml     # File cấu hình trung tâm (Spring, JPA, Mail, Gemini, Cloudflare...)
 │       └─ db/changelog/       # Liquibase database master changelog và các changeset schema/data
 └─ test/                       # 300+ Unit Tests và Integration Tests toàn diện
```

---

## 💻 Yêu cầu hệ thống

- **Java Development Kit (JDK):** Phiên bản **17** trở lên (khuyên dùng OpenJDK 17 hoặc 21).
- **Docker & Docker Compose:** Dùng để chạy SQL Server local nhanh chóng.
- **Maven:** Đi kèm sẵn qua Maven Wrapper (`./mvnw` hoặc `mvnw.cmd`).

---

## 🔧 Hướng dẫn cài đặt & Chạy ứng dụng

### 1. Khởi động SQL Server bằng Docker Compose

Dự án cung cấp sẵn cấu hình SQL Server 2022 trong file `docker-compose.yml`.

Mở terminal tại thư mục gốc của dự án:

```powershell
# Trên Windows (PowerShell):
$env:MSSQL_SA_PASSWORD = "YourStrong@Password123"
docker compose up -d
```

```bash
# Trên macOS / Linux:
export MSSQL_SA_PASSWORD="YourStrong@Password123"
docker compose up -d
```

Lệnh này sẽ khởi động SQL Server lắng nghe tại cổng `1433`.

---

### 2. Cấu hình biến môi trường (.env)

Tạo file `.env` từ file mẫu `.env.example`:

```powershell
# Trên Windows (PowerShell):
Copy-Item .env.example .env
```

```bash
# Trên macOS / Linux:
cp .env.example .env
```

Mở file `.env` và điền các thông số phù hợp:

```ini
# Cổng chạy ứng dụng
PORT=3000

# Kết nối cơ sở dữ liệu SQL Server local
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=HotelNT;encrypt=true;trustServerCertificate=true
DB_USER=sa
DB_PASSWORD=YourStrong@Password123

# Mật khẩu container SQL Server
MSSQL_SA_PASSWORD=YourStrong@Password123

# Cấu hình gửi mail xác thực OTP (dùng App Password của Gmail)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password
MAIL_FROM=your-email@gmail.com

# Chuỗi bí mật ký JWT Token (bắt buộc từ 32 ký tự trở lên)
APP_AUTH_JWT_SECRET=your-super-secret-jwt-key-at-least-32-chars-long

# Đường dẫn API công khai (để chatbot ghép URL ảnh tuyệt đối cho Frontend)
APP_PUBLIC_BASE_URL=http://localhost:3000/api

# Cấu hình AI Provider cho Chatbot (cloudflare | gemini | none)
AI_PROVIDER=none
```

---

### 3. Chạy ứng dụng Spring Boot

Chạy lệnh sau để khởi động Backend:

```powershell
# Trên Windows:
.\mvnw.cmd spring-boot:run
```

```bash
# Trên macOS / Linux:
./mvnw spring-boot:run
```

Khi khởi động lần đầu, **Liquibase** sẽ tự động khởi tạo bảng, quan hệ khóa ngoại và nạp dữ liệu danh mục hành chính (Phường, Loại cơ sở, Tiện ích Nha Trang).

Kiểm tra ứng dụng đã sẵn sàng:
- **Health Check Endpoint:** [http://localhost:3000/api/health](http://localhost:3000/api/health)
- Phản hồi mong đợi: `{"status":"UP"}`

---

## 📖 Tài liệu API (Swagger / OpenAPI)

Toàn bộ thông số Request, Response, Mô hình dữ liệu và các quy chuẩn HTTP Status được tài liệu hóa tự động:

- **Swagger UI trực quan:** [http://localhost:3000/api/swagger-ui.html](http://localhost:3000/api/swagger-ui.html)
- **OpenAPI JSON Spec:** [http://localhost:3000/api/v3/api-docs](http://localhost:3000/api/v3/api-docs)

> Tất cả các API đều có prefix ngữ cảnh là `/api` (ví dụ: `/api/listings`, `/api/auth/signup`, `/api/chatbot/messages`).

---

## 🤖 Cấu hình Chatbot AI

Chatbot hỗ trợ 3 chế độ hoạt động thông qua biến `AI_PROVIDER`:

### Chế độ 1: Cloudflare Workers AI (Khuyên dùng khi host tại Hong Kong / Quốc tế)
Sử dụng mô hình mã nguồn mở hiện đại **Llama 3.1 8B Instruct** tốc độ cao và chi phí tối ưu:
```ini
AI_PROVIDER=cloudflare
CF_ACCOUNT_ID=your_cloudflare_account_id
CF_API_TOKEN=your_cloudflare_workers_ai_token
CF_AI_MODEL=@cf/meta/llama-3.1-8b-instruct
```

### Chế độ 2: Google Gemini AI
Sử dụng mô hình **Gemini 3.8 Flash** với chất lượng ngôn ngữ vượt trội:
```ini
AI_PROVIDER=gemini
GEMINI_API_KEY=AIzaSy...your_gemini_api_key
GEMINI_MODEL=gemini-3.8-flash
# Nếu deploy ở vùng bị Google AI chặn (như Azure HK), cấu hình proxy qua Cloudflare Worker:
# GEMINI_BASE_URL=https://gemini-proxy.your-worker.workers.dev
```

### Chế độ 3: Chế độ Tiêu chuẩn không dùng AI (`none`)
```ini
AI_PROVIDER=none
```
Chatbot sẽ sử dụng bộ phân tích từ khóa NLP nội bộ kết hợp với phản hồi văn bản mẫu, không tốn bất kỳ chi phí API nào mà vẫn đảm bảo độ chính xác 100% trong việc lọc phòng và ngân sách.

---

## 🧪 Chạy kiểm thử (Testing)

Dự án được bảo vệ nghiêm ngặt bởi bộ kiểm thử tự động hơn 300 test cases:

```powershell
# Chạy toàn bộ Unit Tests & Controller Tests:
.\mvnw.cmd test

# Chạy riêng bộ test cho Chatbot và Helper:
.\mvnw.cmd test "-Dtest=Chatbot*Test,UrlHelperTest"
```

---

## ☁️ Triển khai sản xuất (Production / Azure)

Khi triển khai lên nền tảng đám mây (như **Azure App Service**, **AWS**, hay **VPS Linux**):

1. **Biến môi trường trên Cloud:** Cấu hình đầy đủ các biến tương ứng trong `.env` vào mục `Configuration -> Application settings`.
2. **URL Ảnh Công khai:** Thiết lập `APP_PUBLIC_BASE_URL=https://<your-backend-domain>/api` để hình ảnh cơ sở lưu trú được trả về dưới dạng đường dẫn tuyệt đối cho Frontend.
3. **Môi trường Cơ sở dữ liệu:** Cấu hình chuỗi kết nối an toàn (Azure SQL Database hoặc Managed SQL Server) với `encrypt=true;trustServerCertificate=false`.

---

## 📄 Bản quyền & Đóng góp

Dự án được phát triển phục vụ nền tảng du lịch thành phố Nha Trang. Mọi đóng góp và đóng góp mã nguồn (Pull Requests) xin vui lòng tuân thủ quy chuẩn commit và vượt qua toàn bộ kiểm thử trước khi tạo PR.
