# Module Host của Dũng Đạt

Nhánh `feature/host-dat`, xây dựng từ develop `aa4476e`.
Nguồn hợp đồng: API_DESIGN.xlsx và HotelNT_API_Postman.json trong HOTELNT_BE.zip đã được trưởng nhóm gửi. Repo develop tại thời điểm bắt đầu chưa có docs/ chứa bộ tài liệu chung.

## Phạm vi

Tất cả endpoint dưới đây có context path `/api`, chỉ dành cho HOST đang ACTIVE:

| Method | Path | Thành công |
| --- | --- | --- |
| GET | /host/profile | 200, CCCD đã che một phần |
| PATCH | /host/profile | 200, chỉ sửa hoTen/soDienThoai |
| POST | /host/profile/change-password | 200, giữ phiên hiện tại, thu hồi phiên khác |
| GET | /host/listings | 200, phân trang cơ sở của chính Host |
| GET | /host/listings/{maCSLT} | 200, chi tiết, media, checklist, allowedActions |
| POST | /host/listings | 201, tạo DRAFT |
| PUT | /host/listings/{maCSLT} | 200, chỉnh sửa và cập nhật trạng thái khi cần |
| DELETE | /host/listings/{maCSLT} | 200, xóa mềm DRAFT/REJECTED |
| POST | /host/listings/{maCSLT}/submit-review | 200, gửi duyệt khi đủ checklist |
| POST | /host/listings/{maCSLT}/withdraw | 200, PENDING về DRAFT |

`Host_API_Postman.json` là phần Host trích từ collection trưởng nhóm (loại upload/xóa media do Trọng phụ trách). Chỉ dùng token thật sau khi tích hợp Auth; các response trong collection là ví dụ hợp đồng, không phải kết quả đã chạy Postman.

Các DTO dùng tên field từ hợp đồng, không trả entity JPA trực tiếp. Lỗi nghiệp vụ dùng AppException/ErrorCode và response chung. Hai lỗi web riêng của Host (JSON không đọc được, xung đột số điện thoại đồng thời) dùng ApiErrorResponse chung.

## Quy tắc đã xử lý

- Role, tài khoản ACTIVE, quyền sở hữu; ID Host không lấy từ body/header/query của người dùng.
- Không cho sửa email/CCCD, kiểm tra số điện thoại trùng, che CCCD.
- Giá nguyên từ 500.000 đến 100.000.000; tên/địa chỉ bắt buộc; mô tả tối đa 500 ký tự theo schema SQL.
- Tọa độ đi cùng cặp, thuộc vùng 12.1–12.45 / 109.1–109.3; tham chiếu loại/phường/tiện ích phải tồn tại.
- PUT thay thế dữ liệu biểu mẫu: thiếu liên hệ/tiện ích/tọa độ/giá sẽ xóa giá trị cũ tương ứng. Cần FE giữ đầy đủ dữ liệu muốn giữ.
- APPROVED có thay đổi thật về thông tin/liên hệ/tiện ích chuyển PENDING; không thay đổi giữ nguyên trạng thái/timestamp. PENDING khóa sửa.
- Đổi giá cập nhật ngayCapNhatGia; bỏ giá đặt timestamp này về null.
- Checklist gửi duyệt gồm thông tin bắt buộc, tọa độ trong vùng, giá, tối thiểu 3 ảnh và 1 tiện ích.
- DRAFT/REJECTED/HIDDEN gửi duyệt được; chỉ PENDING mới rút duyệt được.
- Các thao tác sửa trạng thái dùng transaction và khóa ghi trên tài khoản rồi cơ sở. Admin/media cần cùng quy ước khóa để tránh ghi đè trạng thái khi tích hợp.

## Điểm tích hợp cần Phú / Trọng / trưởng nhóm nối

### Auth của Phú

Repo chưa có JWT/Spring Security. Module không tự giải mã JWT và không có cơ chế header giả lập.

Auth phải cung cấp bean `HostIdentityResolver` chuyển danh tính ĐÃ XÁC THỰC sang `Identity(accountId, sessionId)`, hoặc bọc servlet principal thành `HostPrincipal`. Nếu chưa nối, các endpoint Host từ chối bằng TOKEN_INVALID; đây là hành vi có chủ đích, không phải API đã đăng nhập chạy được ngay.

Nếu dùng Spring Security Authentication, adapter phải lấy user/session từ principal đã xác thực; không lấy accountId trực tiếp từ request. Cần từ chối anonymous/expired/revoked token. Đổi mật khẩu còn kiểm tra phiên hiện tại trong PhienDangNhap. Auth phải kiểm tra trạng thái thu hồi phiên đối với access token để token cũ thực sự hết quyền sau khi đổi mật khẩu.

### Media của Trọng

DELETE đánh dấu REMOVED và phát sự kiện `HostDtos.ListingDeleted` (chứa danh sách media). Chưa có nơi lưu file hay module media nên chưa xóa file vật lý. Consumer media nên dùng `@TransactionalEventListener(phase = AFTER_COMMIT)` và cleanup idempotent. Media vẫn được giữ trong DB dưới cơ sở REMOVED để có thể truy lại và dọn file khi lỗi; không được để API public hoặc media phục vụ cơ sở REMOVED. Cần Trọng hoàn tất consumer/cleanup trước khi coi yêu cầu xóa file đã hoàn thành.

### Thay đổi dùng chung cần trưởng nhóm review

- pom.xml: thêm spring-security-crypto để bcrypt (không thêm security web filter).
- ErrorCode: bổ sung mã lỗi Host còn thiếu; không sửa mã lỗi hiện có của người khác.
- mvnw: thêm quyền thực thi cho Mac/Linux.
- Không sửa entity, schema, application.yml hoặc repository chung.
- ErrorCode LISTING_REMOVED hiện có HTTP 410 còn tài liệu ghi 409: giữ nguyên mã chung; API Host coi cơ sở REMOVED là 404 khi truy xuất. Cần trưởng nhóm thống nhất với phần media.
- Xóa REJECTED và rút duyệt được triển khai theo collection hiện có; tài liệu vẫn ghi một số điểm chờ BA xác nhận.

## Kiểm chứng

Yêu cầu JDK 17. Chạy test không cần DB:

```sh
./mvnw -Dtest=HostServiceTest,HostControllerTest,HostIdentityTest,ListingControllerTest,GeoUtilsTest test
```

Test Host dùng Mockito/MockMvc: quyền sở hữu, role/khóa tài khoản, đầu vào, định dạng lỗi, checklist, chuyển trạng thái, không thay đổi dữ liệu, giá, tạo/xóa, mật khẩu và danh tính giả mạo. Đây chưa thay thế kiểm thử transaction/JPA trên SQL Server thật.

`./mvnw test` toàn repo hiện còn ListingApiTest có sẵn dùng @SpringBootTest, cần DB. Trên máy kiểm tra Docker daemon chưa chạy, SQL Server chưa bật nên test đó thất bại khi kết nối localhost:1433. Không chỉnh test public của Hoàng để che lỗi này.

Chưa thử Postman với JWT thật và chưa kiểm thử trên SQL Server. Sau khi tích hợp Auth/media và bật DB, cần chạy toàn bộ test, thử hai Host truy cập chéo và kiểm tra file thực tế bị dọn khi xóa.
